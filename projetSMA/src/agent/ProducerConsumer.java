package agent;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.AMSService;
import jade.domain.FIPAAgentManagement.AMSAgentDescription;
import jade.domain.FIPAAgentManagement.SearchConstraints;
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

    private double money = 100.0;
    private double currentProducedProductPrice = 1.0;

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
        System.out.println("\tDEBUG: setup - " + this.getLocalName() + " params: " + producedProduct + "/" + consumedProduct + "/" + productionSpeed + "/" + consumptionSpeed);

        addBehaviour(new CyclicBehaviour(this) {
            /**
             * Behaviour to handle buying - Call For Proposal
             *
             * Get all agents.
             * Send call for proposal message if there is still money and space in the stock.
             */
            @Override
            public void action() {
                if (agentsList == null) {
                    SearchConstraints sc = new SearchConstraints();
                    sc.setMaxResults((long) -1);
                    try {
                        agentsList = AMSService.search(myAgent, new AMSAgentDescription(), sc);
                        System.out.println("\tDEBUG: Buy - " + myAgent.getLocalName() + " set agentList");
                    } catch (FIPAException e) {
                        e.printStackTrace();
                    }
                }

                if (money <= 0 || maxConsumedStock == currentConsumedProductStock) {
                    System.out.println("\tDEBUG: Buy - " + myAgent.getLocalName() + " money=0 or maxStock");
                    block();
                } else {
                    System.out.println("\tDEBUG: Buy - " + myAgent.getLocalName() + " send CFP");
                    ACLMessage msg = new ACLMessage(ACLMessage.CFP);
                    msg.setContent(consumedProduct);
                    for (AMSAgentDescription current : agentsList) {
                        if (!myAgent.getAID().getName().equalsIgnoreCase(current.getName().getName())) {
                            msg.addReceiver(current.getName());
                        }
                    }
                    send(msg);
                }
            }
        });

        addBehaviour(new CyclicBehaviour(this) {
            /**
             * Behaviour to handle buying - Accept or Reject Proposal
             *
             * Send the accept proposal or reject proposal message on receipt of the propose message.
             * TODO
             */
            @Override
            public void action() {
                MessageTemplate messageType = MessageTemplate.MatchPerformative(ACLMessage.PROPOSE);
                ACLMessage msg = receive(messageType);
                if (msg != null) {
                    String content = msg.getContent();
                    String[] contentArray = content.split(" ");
                    int quantity = Integer.parseInt(contentArray[1]);
                    double price = Double.parseDouble(contentArray[2]);
                    double totalPrice = price*quantity;

                    if (quantity >= maxConsumedStock - currentConsumedProductStock) {
                        quantity = maxConsumedStock - currentConsumedProductStock;
                        totalPrice = price*quantity;
                    }

                    if (money < totalPrice || maxConsumedStock == currentConsumedProductStock) {
                        System.out.println("\tDEBUG: Buy - " + myAgent.getLocalName() + " send REJECT");
                        ACLMessage reply = msg.createReply();
                        reply.setPerformative(ACLMessage.REJECT_PROPOSAL);
                        send(reply);
                    } else {
                        System.out.println("\tDEBUG: Buy - " + myAgent.getLocalName() + " send ACCEPT");
                        ACLMessage reply = msg.createReply();
                        reply.setPerformative(ACLMessage.ACCEPT_PROPOSAL);
                        reply.setContent(String.valueOf(quantity));
                        send(reply);

                        money -= totalPrice;
                        currentConsumedProductStock += quantity;
                    }
                }
            }
        });

        addBehaviour(new CyclicBehaviour(this) {
            /**
             * Behaviour to handle selling - Propose
             *
             * Send the propose message on receipt of the call for proposal message, if the require product if the produced product.
             * Manage pricing changes according to money and satisfaction.
             */
            @Override
            public void action() {
                if (satisfaction <= 0.5 && money <= 0) {
                    System.out.println("\tDEBUG: Sell - " + myAgent.getLocalName() + " decrease price");
                    currentProducedProductPrice -= 0.1;
                } else if (satisfaction > 0.5 && money > 0) {
                    System.out.println("\tDEBUG: Sell - " + myAgent.getLocalName() + " increase price");
                    currentProducedProductPrice += 0.1;
                }

                MessageTemplate messageType = MessageTemplate.MatchPerformative(ACLMessage.CFP);
                ACLMessage msg = receive(messageType);
                if (msg != null && msg.getContent().equals(producedProduct)) {
                    System.out.println("\tDEBUG: Sell - " + myAgent.getLocalName() + " send PROPOSE");
                    ACLMessage reply = msg.createReply();
                    reply.setPerformative(ACLMessage.PROPOSE);
                    reply.setContent(producedProduct + " " + currentProducedProductStock + " " + currentProducedProductPrice);
                    send(reply);
                }
            }
        });

        addBehaviour(new CyclicBehaviour(this) {
            /**
             * Behaviour to handle selling - Confirm
             *
             * Send the confirm message on receipt of the accept proposal message.
             */
            @Override
            public void action() {
                MessageTemplate messageType = MessageTemplate.MatchPerformative(ACLMessage.ACCEPT_PROPOSAL);
                ACLMessage msg = receive(messageType);
                if (msg != null) {
                    System.out.println("\tDEBUG: Sell - " + myAgent.getLocalName() + " send CONFIRM");

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

        addBehaviour(new CyclicBehaviour(this) {
            /**
             * Behaviour to handle consumption
             *
             * Consume the product if there still stock.
             * Manage satisfaction according to the stock.
             */
            @Override
            public void action() {
                if (currentConsumedProductStock > 0) {
                    if (satisfaction != 1) {
                        System.out.println("\tDEBUG: Consume - " + myAgent.getLocalName() + " satisfaction=1");
                        satisfaction = 1;
                        averageSatisfaction += satisfaction;
                        satisfactionCount++;
                        averageSatisfactionCount = 1;
                    }

                    if (((System.currentTimeMillis()/1000) - startTime) % consumptionSpeed == 0) {
                        System.out.println("\tDEBUG: Consume - " + myAgent.getLocalName() + " consume");
                        currentConsumedProductStock--;
                    }
                }
                if (currentConsumedProductStock <= 0) {
                    System.out.println("\tDEBUG: Consume - " + myAgent.getLocalName() + " satisfaction decrease");
                    satisfaction = Math.exp(-0.1*averageSatisfactionCount);
                    averageSatisfaction += satisfaction;
                    satisfactionCount++;
                    averageSatisfactionCount++;
                }
            }
        });

        addBehaviour(new CyclicBehaviour(this) {
            /**
             * Behaviour to handle production
             *
             * Produce the product if there is still place in the stock.
             */
            @Override
            public void action() {
                if (maxProducedStock == currentProducedProductStock) {
                    System.out.println("\tDEBUG: Produce - " + myAgent.getLocalName() + " maximum stock");
                    block();
                } else {
                    if (((System.currentTimeMillis()/1000) - startTime) % productionSpeed == 0) {
                        System.out.println("\tDEBUG: Produce - " + myAgent.getLocalName() + " produce");
                        currentProducedProductStock++;
                    }
                }
            }
        });

        addBehaviour(new CyclicBehaviour(this) {
            /**
             * Behaviour to handle end of simulation and die
             *
             * Died if satisfaction is under 0.2 or if it receives end simulation message.
             */
            @Override
            public void action() {
                if (satisfaction < 0.2) {
                    System.out.println("\tDEBUG: Died Satisfaction - " + myAgent.getLocalName());
                    doDelete();
                }
                MessageTemplate messageType = MessageTemplate.MatchPerformative(ACLMessage.INFORM);
                ACLMessage msg = receive(messageType);
                if (msg != null) {
                    System.out.println("\tDEBUG: End simulation - " + myAgent.getLocalName());
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
