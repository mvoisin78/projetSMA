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
    private int satisfactionCount;
    private int averageSatisfactionCount;

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
        this.satisfaction = 1.0;
        this.averageSatisfaction = 1.0;
        this.satisfactionCount = 1;
        this.averageSatisfactionCount = 1;
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
                e.printStackTrace();
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

                if (money <= 0 || maxConsumedStock == currentConsumedProductStock) {
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
                        System.out.println("\tDEBUG: Reject - " + myAgent.getLocalName());
                        ACLMessage reply = msg.createReply();
                        reply.setPerformative(ACLMessage.REJECT_PROPOSAL);
                        send(reply);
                    } else {
                        System.out.println("\tDEBUG: Accept - " + myAgent.getLocalName());
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

        // Propose Sell Behaviour
        addBehaviour(new CyclicBehaviour(this) {
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

        // Confirm Sell Behaviour
        addBehaviour(new CyclicBehaviour(this) {
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

        // Consume Behaviour
        addBehaviour(new CyclicBehaviour(this) {
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
                    doDelete();
                }
            }
        });
    }

    @Override
    protected void takeDown() {
        System.out.println("I'm Agent: " + this.getLocalName());
        System.out.println("Average Satisfaction: " + this.averageSatisfaction/this.satisfactionCount);
    }
}
