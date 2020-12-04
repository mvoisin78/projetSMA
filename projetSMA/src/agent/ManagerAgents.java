/*
 * ManagerAgents parameters : 3,abc,bca,20
 */
package agent;

import java.util.ArrayList;
import java.util.Random;

import jade.core.Agent;
import jade.wrapper.AgentContainer;
import jade.wrapper.AgentController;
import jade.wrapper.StaleProxyException;
import behaviour.*;

public class ManagerAgents extends Agent {
	protected void setup() {
		Object[] args = getArguments();

		int agentsCount = 3;
		String agentsProducedProduct = "ABC";
		String agentsConsumedProduct = "BCA";
		int simulationDuration = 100;

		if (args != null) {
			if (args.length == 4) {
				agentsCount = Integer.parseInt(args[0].toString());
				agentsProducedProduct = args[1].toString();
				agentsConsumedProduct = args[2].toString();
				simulationDuration = Integer.parseInt(args[3].toString());
			} else {
				System.err.println("Missing arguments");
			}
		}
		printArguments(agentsCount, agentsProducedProduct, agentsConsumedProduct, simulationDuration);

		if (agentsCount == agentsProducedProduct.length() && agentsCount == agentsConsumedProduct.length()) {
			AgentContainer c = getContainerController();
			Random r = new Random();
			String name;
			ArrayList<AgentController> agents = new ArrayList<>();
			for (int index = 0; index < agentsCount; index++) {
				Object[] agentsArgs = new Object[4];
				agentsArgs[0] = agentsProducedProduct.charAt(index);
				agentsArgs[1] = agentsConsumedProduct.charAt(index);
				agentsArgs[2] = r.nextInt(6) + 5;
				agentsArgs[3] = r.nextInt(6) + 5;
				name = agentsArgs[0].toString() + index;

				try {
					AgentController ac = c.createNewAgent(name, "agent.ProducerConsumer", agentsArgs);
					agents.add(ac);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			startAll(agents);

		} else {
			System.err.println("Invalid parameters' count");
		}

		addBehaviour(new StopBehaviour(this, simulationDuration));
	}

	void startAll(ArrayList<AgentController> agents) {
		System.out.println("Start agents");
		for (AgentController agent : agents) {
			try {
				agent.start();
			} catch (StaleProxyException e) {
				e.printStackTrace();
			}
		}
	}

	void printArguments(int agentsCount, String agentsProducedProduct, String agentsConsumedProduct, int simulationDuration) {
		System.out.println("Agents' count: " + agentsCount);
		System.out.println("Agents' produced product: " + agentsProducedProduct);
		System.out.println("Agents' consumed product: " + agentsConsumedProduct);
		System.out.println("Simulation duration: " + simulationDuration);
	}
}
