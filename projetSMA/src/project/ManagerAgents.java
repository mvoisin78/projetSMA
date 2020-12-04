/**
 * 
 * Param�tres de cr�ation du ManagerAgents : 3,abc,bca,20
 * 
 */
package project;

import java.util.ArrayList;
import java.util.Random;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.SimpleBehaviour;
import jade.domain.AMSService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.AMSAgentDescription;
import jade.domain.FIPAAgentManagement.SearchConstraints;
import jade.lang.acl.ACLMessage;
import jade.wrapper.AgentContainer;
import jade.wrapper.AgentController;
import jade.wrapper.StaleProxyException;

public class ManagerAgents extends Agent {

	protected void setup() {

		// extracting optional number of iterations
		Object[] args = getArguments();

		int nbAgents = 3;
		String agentsType = "ABC";
		String agentsCons = "BCA";
		int simuDuration = 100;

		if (args != null) {
			if (args.length == 4) {
				nbAgents = Integer.parseInt(args[0].toString());
				agentsType = args[1].toString();
				agentsCons = args[2].toString();
				simuDuration = Integer.parseInt(args[3].toString());
				printArgs(nbAgents, agentsType, agentsCons, simuDuration);
			} else {
				System.out.println("Des param�tres par d�faut ont �t� mis en absence d'arguments valides");
				printArgs(nbAgents, agentsType, agentsCons, simuDuration);
			}
		}

		if (nbAgents == agentsType.length() && nbAgents == agentsCons.length()) {
			AgentContainer c = getContainerController();
			Random r = new Random();
			String name;
			ArrayList<AgentController> agents = new ArrayList<>();
			for (int index = 0; index < nbAgents; index++) {
				Object[] agentsArgs = new Object[4];
				agentsArgs[0] = agentsType.charAt(index);
				agentsArgs[1] = agentsCons.charAt(index);
				agentsArgs[2] = r.nextInt(6) + 5;
				agentsArgs[3] = r.nextInt(6) + 5;

				System.out.println("vitesse prod : " + agentsArgs[2]);
				System.out.println("vitesse cons : " + agentsArgs[3]);

				name = agentsArgs[0].toString() + index;
				System.out.println(name);

				try {
					// AgentController ac = c.createNewAgent(name, "project.ProducerConsumer",
					// agentsArgs);
					// agents.add(ac);
				} catch (Exception e) {
					// TODO: handle exception
				}
			}
			startAll(agents);

		} else {
			System.out.println("Param�tres non valides, merci de r��sayer");
		}

		addBehaviour(new StopBehavior(this, simuDuration));

	}

	void startAll(ArrayList<AgentController> agents) {
		System.out.println("D�marage des agents");
		for (AgentController agent : agents) {
			try {
				agent.start();
			} catch (StaleProxyException e) {
				e.printStackTrace();
			}
		}
	}

	void printArgs(int nbAgents, String agentsType, String agentsCons, int simuDuration) {
		System.out.println("Nombre d'agents : " + nbAgents);
		System.out.println("Type des agents :" + agentsType);
		System.out.println("Consomation des agents :" + agentsCons);
		System.out.println("Dur�e de la simulation : " + simuDuration + " unit�es de temps");
	}

}

class StopBehavior extends SimpleBehaviour {

	protected int duration;
	Agent a;

	public StopBehavior(Agent a, int simuDuration) {
		this.a = a;
		duration = simuDuration;
	}

	public void action() {
		try {
			Thread.sleep(100);
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
				msg.setContent(" PING ");
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
