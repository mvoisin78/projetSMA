package agent;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.AMSService;
import jade.domain.FIPAAgentManagement.AMSAgentDescription;
import jade.domain.FIPAAgentManagement.SearchConstraints;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

public class ProducerConsumer extends Agent {
    private final long startTime =  System.currentTimeMillis()/1000;

    private AMSAgentDescription[] agentsList = null;

    private double satisfaction;
    private double averageSatisfaction;

    private String producedProduct;
    private String consumedProduct;
    private int productionSpeed;
    private int consumptionSpeed;
    private int currentProducedProductStock;
    private int currentConsumedProductStock;
    private int maxProducedStock;
    private int maxConsumedStock;

    private double money;
    private double currentProducedProductPrice;


    protected void setup() {
        System.out.println("\tDEBUG: setup - " + this.getLocalName());

        this.satisfaction = 1.0;
        this.currentProducedProductPrice = 1.0;
        this.money = 100.0;

        // Get arguments
        Object[] args = getArguments();
        if (args != null && args.length != 0) {
            try {
                this.producedProduct = args[0].toString();
                this.consumedProduct = args[1].toString();
                this.productionSpeed = Integer.parseInt(args[2].toString());
                this.consumptionSpeed = Integer.parseInt(args[3].toString());
            } catch (Exception e) {
                e.getStackTrace();
            }
        }
        System.out.println("\tDEBUG: setup - " + this.getLocalName() + " params: " + producedProduct + "/" + consumedProduct + "/" + productionSpeed + "/" + consumptionSpeed);

        // Call For Proposal Buy Behaviour
        addBehaviour(new CyclicBehaviour(this) {
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

                if (money <= 0) {
                    System.out.println("\tDEBUG: Buy - " + myAgent.getLocalName() + " money=0");
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

        // Accept or Reject Proposal Buy Behaviour
        addBehaviour(new CyclicBehaviour(this) {
            @Override
            public void action() {
                //ACCEPT_PROPOSAL(quantité)
                //REJECT_PROPOSAL, trouvé moins cher ailleurs
                MessageTemplate messageType = MessageTemplate.MatchPerformative(ACLMessage.PROPOSE);
                ACLMessage msg = receive(messageType);
                if (msg != null) {
                    System.out.println("\tDEBUG: Accept or Reject - " + myAgent.getLocalName());
                    //TODO
                }
            }
        });

        // Propose Sell Behaviour
        addBehaviour(new CyclicBehaviour(this) {
            @Override
            public void action() {
                if (satisfaction <= 0.5 && money <= 0) {
                    System.out.println("\tDEBUG: Sell - " + myAgent.getLocalName() + " increase price");
                    currentProducedProductPrice -= 0.1;
                } else if (satisfaction > 0.5 && money > 0) {
                    System.out.println("\tDEBUG: Sell - " + myAgent.getLocalName() + " decrease price");
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

        // Confirm Sell Behaviour
        addBehaviour(new CyclicBehaviour(this) {
            @Override
            public void action() {
                MessageTemplate messageType = MessageTemplate.MatchPerformative(ACLMessage.ACCEPT_PROPOSAL);
                ACLMessage msg = receive(messageType);
                if (msg != null) {
                    System.out.println("\tDEBUG: Sell - " + myAgent.getLocalName() + " send CONFIRM");
                    //TODO transferer marchandise + money
                    ACLMessage reply = msg.createReply();
                    reply.setPerformative(ACLMessage.CONFIRM);
                    send(reply);
                }
            }
        });

        // Consume Behaviour
        addBehaviour(new CyclicBehaviour(this) {
            @Override
            public void action() {
                if (currentConsumedProductStock > 0) {
                    if (satisfaction != 1) {
                        System.out.println("\tDEBUG: Consume - " + myAgent.getLocalName() + " satisfaction=1");
                        satisfaction = 1;
                        //TODO averageSatisfaction
                    }

                    if (((System.currentTimeMillis()/1000) - startTime) % consumptionSpeed == 0) {
                        System.out.println("\tDEBUG: Consume - " + myAgent.getLocalName() + " consume");
                        currentConsumedProductStock--;
                    }
                }
                if (currentConsumedProductStock <= 0) {
                    System.out.println("\tDEBUG: Consume - " + myAgent.getLocalName() + " satisfaction decrease");
                    //TODO satisfaction = ; exp(-lambda*t)
                    //TODO averageSatisfaction
                }
            }
        });

        // Produce Behaviour
        addBehaviour(new CyclicBehaviour(this) {
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


        // End Simulation Behaviour
        addBehaviour(new CyclicBehaviour(this) {
            @Override
            public void action() {
                MessageTemplate messageType = MessageTemplate.MatchPerformative(ACLMessage.INFORM);
                ACLMessage msg = receive(messageType);
                if (msg != null) {
                    System.out.println("\tDEBUG: End simulation - " + myAgent.getLocalName());
                    takeDown();
                }
            }
        });
    }

    @Override
    protected void takeDown() {
        super.takeDown();

        System.out.println("I'm Agent: " + this.getLocalName());
        System.out.println("Average Satisfaction" + this.averageSatisfaction);
    }
}
