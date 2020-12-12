package agent;

import java.util.ArrayList;

import javax.sql.rowset.serial.SerialStruct;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.AMSService;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.AMSAgentDescription;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.SearchConstraints;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

/**
 * Producer and Consumer Agent
 */
public class ProducerConsumer extends Agent {
    private final long startTime =  System.currentTimeMillis()/1000;

    private AMSAgentDescription[] agentsList = null;

    private double satisfaction = 1.0;
    private double averageSatisfaction = 1.0;
    private int satisfactionCount = 1;
    private int averageSatisfactionCount = 1;

    private String producedProduct;
    private String consumedProduct;
    private int productionSpeed;
    private int consumptionSpeed;
    private int currentProducedProductStock = 0;
    private int currentConsumedProductStock = 0;
    private final int maxProducedStock = 10;
    private final int maxConsumedStock = 10;

    private int countMessage = 0;
    private int nbAgent = 0;
    ArrayList<ACLMessage> messageList = new ArrayList<>();
    
    private double money = 100.0;
    private double currentProducedProductPrice = 1.0;
    
    AID agents[];

    /**
     * Initialize variables and set behaviours
     */
    protected void setup() {
        Object[] args = getArguments();
        if (args != null && args.length != 0) {
            try {
                this.producedProduct = args[0].toString();
                this.consumedProduct = args[1].toString();
                this.productionSpeed = Integer.parseInt(args[2].toString());
                this.consumptionSpeed = Integer.parseInt(args[3].toString());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        System.out.println("setup - " + this.getLocalName() + " params: " + producedProduct + "/" + consumedProduct + "/" + productionSpeed + "/" + consumptionSpeed);

        
        register();
        
        searchBehaviour();
        
        CFPBehaviour();
        
        responseBehaviour();

        proposeBehaviour();

        confirmBehaviour();

        consumeBehaviour();

        produceBehaviour();

        endBehaviour();
    }
    
    /**
     * Register agent into the DFService
     */
    private void register() {
    	 DFAgentDescription dfd = new DFAgentDescription();
         dfd.setName(getAID());
         
         ServiceDescription sd = new ServiceDescription();
         sd.setType(producedProduct);
         sd.setName(getLocalName());
         dfd.addServices(sd);
         try {
         	DFService.register(this,dfd);
         }catch(FIPAException fe) {
         	fe.printStackTrace();
         }
    }
    
    /**
     * Add Behaviour to search agents
     *
     * Put all agents who sends the good product in a list
     */
    private void searchBehaviour() {
    	 addBehaviour(new CyclicBehaviour(this) {
            
             @Override
             public void action() {
             	MessageTemplate messageType = MessageTemplate.MatchPerformative(ACLMessage.SUBSCRIBE);
                 ACLMessage msg = receive(messageType);
 	            if (msg != null) {
 	                DFAgentDescription template = new DFAgentDescription();
 	                ServiceDescription sd = new ServiceDescription();
 	                sd.setType(consumedProduct);
 	                template.addServices(sd);
 	                try {
 	                	DFAgentDescription[] result = DFService.search(myAgent, template);
 	                	agents = new AID[result.length];
 	                	for(int i=0; i<result.length; i++) {
 	                		agents[i] = result[i].getName();
 	                	}
 	                } catch(FIPAException fe) {
 	                	fe.printStackTrace();
 	                }
 	                nbAgent = agents.length;
 	                System.err.println("RECHERCHE EFFECTUEE " + nbAgent);
 	            }
             }
         });
    }

    /**
     * Add Behaviour to handle buying - Call For Proposal
     *
     * Get all agents.
     * Send call for proposal message if there is still money and space in the stock.
     */
    private void CFPBehaviour() {
    	addBehaviour(new CyclicBehaviour(this) {
            
            @Override
            public void action() {
               if(agents != null) {
	                if (money <= 0 || maxConsumedStock == currentConsumedProductStock) {
	                    System.out.println("Buy - " + myAgent.getLocalName() + " money=0 or maxStock");
	                    block();
	                } else {
	                    System.out.println("Buy - " + myAgent.getLocalName() + " send CFP");
	                    ACLMessage msg = new ACLMessage(ACLMessage.CFP);
	                    msg.setContent(consumedProduct);
	                    for (AID agent : agents) {
	                    	msg.addReceiver(agent);
	                    }
	                    send(msg);
	                }
               }
            }
        });
    }
    
    /**
     * Add Behaviour to handle buying - Accept or Reject Proposal
     *
     * Send the accept proposal or reject proposal message on receipt of the propose message.
     * TODO
     */
    private void responseBehaviour() {
    	addBehaviour(new CyclicBehaviour(this) {
           
            @Override
            public void action() {
                MessageTemplate messageType = MessageTemplate.MatchPerformative(ACLMessage.PROPOSE);
                ACLMessage msg = receive(messageType);
                if (msg != null) {
                	countMessage ++;
                	messageList.add(msg);

	                if(nbAgent >= countMessage) {
	                	ACLMessage bestProposal = new ACLMessage();
	                    double bestOffer = 9999;
	                    int bestQuantity = 0;
	                	for(ACLMessage message : messageList) {
		                	String content = message.getContent();
		                    String[] contentArray = content.split(" ");
		                    int quantity = Integer.parseInt(contentArray[1]);
		                    double price = Double.parseDouble(contentArray[2]);
		                    double totalPrice = price*quantity;
		                    
		                    if (quantity >= maxConsumedStock - currentConsumedProductStock) {
		                        quantity = maxConsumedStock - currentConsumedProductStock;
		                        totalPrice = price*quantity;
		                    }
		                    if (money > totalPrice && maxConsumedStock != currentConsumedProductStock) {
		                    	
		                    	if(price<bestOffer) {
		                    		bestProposal = message;
		                    		bestOffer = price;
		                    		bestQuantity = quantity;
			                    }
		                    }
	                	}
	                	for(ACLMessage message : messageList){
	                		if(message.equals(bestProposal)) {
		                		System.out.println("Buy - " + myAgent.getLocalName() + " send ACCEPT");
		                        ACLMessage reply = bestProposal.createReply();
		                        reply.setPerformative(ACLMessage.ACCEPT_PROPOSAL);
		                        reply.setContent(String.valueOf(bestQuantity));
		                        send(reply);
		
		                        money -= bestQuantity*bestOffer;
		                        currentConsumedProductStock += bestQuantity;
	                		}
	                		else {
	                			System.out.println("Buy - " + myAgent.getLocalName() + " send REJECT");
		                        ACLMessage reply = msg.createReply();
		                        reply.setPerformative(ACLMessage.REJECT_PROPOSAL);
		                        send(reply);
	                		}
	                	}
	                	countMessage = 0;
	                	messageList.clear();
                	}
	                else {
	                	System.err.println("Nb attendu : " + nbAgent + " Nb recu : " + countMessage);
	                }
                }
            }
        });
    }
    
    /**
     * Add Behaviour to handle selling - Propose
     *
     * Send the propose message on receipt of the call for proposal message, if the require product if the produced product.
     * Manage pricing changes according to money and satisfaction.
     */
    private void proposeBehaviour() {
    	addBehaviour(new CyclicBehaviour(this) {
            @Override
            public void action() {
                if (satisfaction <= 0.5 && money <= 0) {
                    System.out.println("Sell - " + myAgent.getLocalName() + " decrease price");
                    currentProducedProductPrice -= 0.1;
                } else if (satisfaction > 0.5 && money > 0) {
                    System.out.println("Sell - " + myAgent.getLocalName() + " increase price");
                    currentProducedProductPrice += 0.1;
                }

                MessageTemplate messageType = MessageTemplate.MatchPerformative(ACLMessage.CFP);
                ACLMessage msg = receive(messageType);
                if (msg != null && msg.getContent().equals(producedProduct)) {
                    System.out.println("Sell - " + myAgent.getLocalName() + " send PROPOSE");
                    ACLMessage reply = msg.createReply();
                    reply.setPerformative(ACLMessage.PROPOSE);
                    reply.setContent(producedProduct + " " + currentProducedProductStock + " " + currentProducedProductPrice);
                    send(reply);
                }
            }
        });
    }
    
    /**
     * Add Behaviour to handle selling - Confirm
     *
     * Send the confirm message on receipt of the accept proposal message.
     */
    private void confirmBehaviour() {
    	addBehaviour(new CyclicBehaviour(this) {
            
            @Override
            public void action() {
                MessageTemplate messageType = MessageTemplate.MatchPerformative(ACLMessage.ACCEPT_PROPOSAL);
                ACLMessage msg = receive(messageType);
                if (msg != null) {
                    System.out.println("Sell - " + myAgent.getLocalName() + " send CONFIRM");

                    String content = msg.getContent();
                    int quantity = Integer.parseInt(content);

                    ACLMessage reply = msg.createReply();
                    reply.setPerformative(ACLMessage.CONFIRM);
                    send(reply);

                    currentProducedProductStock -= quantity;
                    money += quantity*currentProducedProductPrice;
                }
            }
        });
    }
    
    /**
     * Add Behaviour to handle consumption
     *
     * Consume the product if there still stock.
     * Manage satisfaction according to the stock.
     */
    private void consumeBehaviour() {
    	addBehaviour(new CyclicBehaviour(this) {
            
            @Override
            public void action() {
                if (currentConsumedProductStock > 0) {
                    if (satisfaction != 1) {
                        System.out.println("Consume - " + myAgent.getLocalName() + " satisfaction=1");
                        satisfaction = 1;
                        averageSatisfaction += satisfaction;
                        satisfactionCount++;
                        averageSatisfactionCount = 1;
                    }

                    if (((System.currentTimeMillis()/1000) - startTime) % consumptionSpeed == 0) {
                        System.out.println("Consume - " + myAgent.getLocalName() + " consume");
                        currentConsumedProductStock--;
                    }
                }
                if (currentConsumedProductStock <= 0) {
                    System.out.println("Consume - " + myAgent.getLocalName() + " satisfaction decrease");
                    satisfaction = Math.exp(-0.1*averageSatisfactionCount);
                    averageSatisfaction += satisfaction;
                    satisfactionCount++;
                    averageSatisfactionCount++;
                }
            }
        });
    }
    
    /**
     * Add Behaviour to handle production
     *
     * Produce the product if there is still place in the stock.
     */
    private void produceBehaviour() {
    	addBehaviour(new CyclicBehaviour(this) {
            
            @Override
            public void action() {
                if (maxProducedStock == currentProducedProductStock) {
                    System.out.println("Produce - " + myAgent.getLocalName() + " maximum stock");
                    block();
                } else {
                    if (((System.currentTimeMillis()/1000) - startTime) % productionSpeed == 0) {
                        System.out.println("Produce - " + myAgent.getLocalName() + " produce");
                        currentProducedProductStock++;
                    }
                }
            }
        });
    }
    
    /**
     * Add Behaviour to handle end of simulation and die
     *
     * Died if satisfaction is under 0.2 or if it receives end simulation message.
     */
    private void endBehaviour() {
    	addBehaviour(new CyclicBehaviour(this) {
            
            @Override
            public void action() {
                if (satisfaction < 0.2) {
                    System.out.println("Died Satisfaction - " + myAgent.getLocalName());
                    try {
						DFService.deregister(myAgent);
					} catch (FIPAException e) {
						e.printStackTrace();
					}
                    doDelete();
                }
                MessageTemplate messageType = MessageTemplate.MatchPerformative(ACLMessage.INFORM);
                ACLMessage msg = receive(messageType);
                if (msg != null) {
                    System.out.println("End simulation - " + myAgent.getLocalName());
                    try {
						DFService.deregister(myAgent);
					} catch (FIPAException e) {
						e.printStackTrace();
					}
                    doDelete();
                }
            }
        });
    }
    
    /**
     * Method called when Agent is killed to print information
     */
    @Override
    protected void takeDown() {
        System.out.println("I'm Agent: " + this.getLocalName());
        System.out.println("Average Satisfaction: " + this.averageSatisfaction/this.satisfactionCount);
    }
}
