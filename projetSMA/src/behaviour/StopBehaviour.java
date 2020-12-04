package behaviour;

import jade.core.Agent;
import jade.core.behaviours.SimpleBehaviour;
import jade.domain.AMSService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.AMSAgentDescription;
import jade.domain.FIPAAgentManagement.SearchConstraints;
import jade.lang.acl.ACLMessage;

public class StopBehaviour extends SimpleBehaviour {

	protected int duration;
	Agent a;

	public StopBehaviour(Agent a, int simuDuration) {
		this.a = a;
		duration = simuDuration;
	}

	public void action() {
		try {
			Thread.sleep(10);
		} catch (InterruptedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		--duration;
		System.out.println(duration);
		if (duration == 0) {
			AMSAgentDescription[] agents = null;
			try {
				SearchConstraints c = new SearchConstraints();
				c.setMaxResults(new Long(-1));
				agents = AMSService.search(a, new AMSAgentDescription(), c);
			} catch (FIPAException e) {
				e.printStackTrace();
			}

			for (AMSAgentDescription agent : agents) {
				ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
				msg.setContent("STOP");
				if (!agent.getName().equals(a.getAID())) {
					msg.addReceiver(agent.getName());
					a.send(msg);
				}
			}
			System.out.println("fin de la simu");
		}
	}

	@Override
	public boolean done() {
		return duration == 0;
	}

}