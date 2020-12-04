package agent;

import java.util.ArrayList;
import java.util.Random;
import jade.core.Agent;
import jade.wrapper.AgentContainer;
import jade.wrapper.AgentController;
import jade.wrapper.StaleProxyException;
import behaviour.*;

/**
 * Manager Agent of Producer and Consumer
 *
 * Parameters: 3,abc,bca,20
 */
public class ManagerAgents extends Agent {
	private int agentsCount = 3;
	private String agentsProducedProducts = "ABC";
	private String agentsConsumedProducts = "BCA";
	private int simulationDuration = 100;

	ArrayList<AgentController> agents = new ArrayList<>();

	/**
	 * Initialize variables, create Agents and set behaviour
	 */
	protected void setup() {
		Object[] args = getArguments();
		if (args != null) {
			if (args.length == 4) {
				agentsCount = Integer.parseInt(args[0].toString());
				agentsProducedProducts = args[1].toString();
				agentsConsumedProducts = args[2].toString();
				simulationDuration = Integer.parseInt(args[3].toString());
			} else {
				System.err.println("Missing arguments, needed: agentsCount agentsProducedProducts agentsConsumedProducts simulationDuration.");
			}
		}
		printArguments();

		if (agentsCount == agentsProducedProducts.length() && agentsCount == agentsConsumedProducts.length()) {
			AgentContainer c = getContainerController();
			Random r = new Random();
			String name;

			for (int index = 0; index < agentsCount; index++) {
				Object[] agentsArgs = new Object[4];
				agentsArgs[0] = agentsProducedProducts.charAt(index);
				agentsArgs[1] = agentsConsumedProducts.charAt(index);
				agentsArgs[2] = r.nextInt(4) + 1;
				agentsArgs[3] = r.nextInt(4) + 1;
				name = agentsArgs[0].toString() + index;

				try {
					AgentController ac = c.createNewAgent(name, "agent.ProducerConsumer", agentsArgs);
					agents.add(ac);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			startAllAgents();
		} else {
			System.err.println("Invalid parameters' count");
		}

		addBehaviour(new StopBehaviour(this, simulationDuration));
	}

	/**
	 * Start all Agents
	 */
	private void startAllAgents() {
		System.out.println("Start agents");
		for (AgentController agent : agents) {
			try {
				agent.start();
			} catch (StaleProxyException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Print simulation's arguments
	 */
	private void printArguments() {
		System.out.println("Agents' count: " + agentsCount);
		System.out.println("Agents' produced product: " + agentsProducedProducts);
		System.out.println("Agents' consumed product: " + agentsConsumedProducts);
		System.out.println("Simulation duration: " + simulationDuration);
	}
}
