package behaviour;

import jade.core.Agent;
import jade.core.behaviours.SimpleBehaviour;
import jade.domain.AMSService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.AMSAgentDescription;
import jade.domain.FIPAAgentManagement.SearchConstraints;
import jade.lang.acl.ACLMessage;

/**
 * Behaviour to handle simulation and stop condition
 */
public class StopBehaviour extends SimpleBehaviour {
	protected int duration;

	/**
	 * Constructor for StopBehaviour
	 *
	 * @param agent, the caller Agent
	 * @param simulationDuration, the simulation duration
	 */
	public StopBehaviour(Agent agent, int simulationDuration) {
		super(agent);
		duration = simulationDuration;
	}

	/**
	 * The action of the behaviour
	 *
	 * Handles the simulation time by decrementing the simulation duration.
	 * When the simulation is over, it sends a message to all agents to tell them to kill themselves.
	 */
	public void action() {
		try {
			Thread.sleep(1);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		--duration;
		System.out.println("Duration=" + duration);
		if (duration == 0) {
			AMSAgentDescription[] agents = null;
			try {
				SearchConstraints c = new SearchConstraints();
				c.setMaxResults((long) -1);
				agents = AMSService.search(myAgent, new AMSAgentDescription(), c);
			} catch (FIPAException e) {
				e.printStackTrace();
			}

			if (agents != null) {
				ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
				for (AMSAgentDescription agent : agents) {
					if (!myAgent.getAID().getName().equalsIgnoreCase(agent.getName().getName())) {
						msg.addReceiver(agent.getName());
					}
				}
				myAgent.send(msg);
				System.out.println("End Simulation");
			}
		}
	}

	/**
	 * Check if the simulation is over to stop the behaviour
	 *
	 * @return boolean, true if the simulation is over, false if not
	 */
	@Override
	public boolean done() {
		return duration == 0;
	}
}