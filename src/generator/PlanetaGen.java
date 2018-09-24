package generator;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import utils.Pair;

public class PlanetaGen {

	private double length;
	private int processors;
	private int tasks;
	private int fanout;
	private int maxdist;

	private List<List<Double>> timelines;
	private List<GTask> allTasks;
	private Set<Pair> dependencies;
	private long seed = 1234567890L;
	private Random randomGenerator;

	public PlanetaGen(double l, int p, int t, int f, int m) {
		length = l;
		processors = p;
		tasks = t;
		fanout = f;
		maxdist = m;
		timelines = new ArrayList<>();
		for (int i = 0; i < p; i++) {
			List<Double> aTimeline = new ArrayList<Double>();
			aTimeline.add(0.0);
			aTimeline.add(length);
			timelines.add(aTimeline);
		}
		allTasks = new ArrayList<>();
		dependencies = new HashSet<>();
		randomGenerator = new Random(seed);
	}

	public void setSeed(long seed) {
		this.seed = seed;
		randomGenerator = new Random(seed);
	}

	public void generate() {
		int t_id=1;
		for (int p = 0; p < processors; p++) {
			int number_of_tasks = randomGenerator.nextInt(tasks / processors) + 1;
			for (int j = 0; j < number_of_tasks; j++) {
				double time = randomGenerator.nextDouble() * length;
				timelines.get(p).add(time);
			}
			Collections.sort(timelines.get(p));
			for (int i = 0; i < timelines.get(p).size() - 1; i++) {
				GTask aTask = new GTask();
				aTask.id = t_id;
				t_id++;
				aTask.cpu = p;
				aTask.start = timelines.get(p).get(i);
				aTask.end = timelines.get(p).get(i + 1);
				allTasks.add(aTask);
			}
		}

		for (GTask task1 : allTasks) {
			int task1_i = allTasks.indexOf(task1);
			int fo = new Random().nextInt(fanout) + 1;
			for (int i = 0; i < fo; i++) {
				boolean retry = true;
				int task2_i = -1;
				GTask task2 = null;
				while (retry) {
					task2_i = randomGenerator.nextInt(allTasks.size());
					task2 = allTasks.get(task2_i);
					if (task2.start >= task1.end || task1.start >= task2.end) {
						double d1 = Math.abs(task2.start - task1.end);
						double d2 = Math.abs(task1.start - task2.end);
						double min = d1;
						if (d2 < min)
							min = d2;
						if (min > length / maxdist)
							continue;
						retry = false;
					}
				}
				if (task1_i < task2_i) {
					Pair pair = new Pair(task1_i + 1, task2_i + 1);
					dependencies.add(pair);
					task1.addSuccessor(task2);
					task2.addPredecessor(task1);
				} else {
					Pair pair = new Pair(task2_i + 1, task1_i + 1);
					dependencies.add(pair);
					task2.addSuccessor(task1);
					task1.addPredecessor(task2);
				}
			}
		}

		// add start and end tasks;
		GTask start = new GTask();
		start.id = 0;
		start.cpu = 0;
		start.start = 0.0;
		start.end = 0.0;
		GTask end = new GTask();
		end.id = allTasks.size() + 1;
		end.cpu = 0;
		end.start = 0.0;
		end.end = 0.0;
		for (GTask task1 : allTasks) {
			if (task1.successors.isEmpty()) {
				task1.addSuccessor(end);
				dependencies.add(new Pair(task1.id, end.id));
			}
			if (task1.predecessors.isEmpty()) {
				task1.addPredecessor(start);
				dependencies.add(new Pair(start.id, task1.id));
			}
		}
		allTasks.add(end);
		allTasks.add(0, start);
	}

	public void printDetails() {
		StringBuffer sb = new StringBuffer();
		sb.append("Tasks(" + allTasks.size() + "):\n");
		for (GTask t : allTasks) {
			sb.append(String.format("%.1f-%.1f(%d) ", t.start, t.end, t.cpu));
		}
		sb.append("\nDependencies(" + dependencies.size() + "):\n");
		List<Pair> deps = new ArrayList<>(dependencies);
		Collections.sort(deps);
		for (Pair pair : deps) {
			sb.append(String.format("%d->%d ", pair.x, pair.y));
		}
		System.out.println(sb.toString());
	}

	public void exportToDOT(String name) {
		StringBuilder sb = new StringBuilder();
		sb.append("digraph ");
		sb.append(name);
		sb.append(" {\n");
		List<Pair> deps = new ArrayList<>(dependencies);
		Collections.sort(deps);
		for (Pair p : deps) {
			sb.append(String.format("%d -> %d;\n", p.x, p.y));
		}
		sb.append(" }\n");
		System.out.println(sb.toString());
//		PrintWriter out;
//		try {
//			out = new PrintWriter(name + ".dot");
//			out.println(sb.toString());
//			out.close();
//		} catch (FileNotFoundException e) {
//			e.printStackTrace();
//		}
	}

	private static void small_instance() {
		double length = 100.0;
		int processors = 3;
		int tasks = 20;
		int fanout = 3;
		int maxDistanceFactor = 10;
		PlanetaGen generator = new PlanetaGen(length, processors, tasks, fanout, maxDistanceFactor);
		generator.generate();
		generator.printDetails();
		generator.exportToDOT("small");
	}

	private static void big_instance() {
		double length = 1000000.0;
		int processors = 4;
		int tasks = 100;
		int fanout = 6;
		int maxDistanceFactor = 7;
		PlanetaGen generator = new PlanetaGen(length, processors, tasks, fanout, maxDistanceFactor);
		generator.generate();
		generator.printDetails();
	}

	public static void main(String[] args) {
		small_instance();
//		big_instance();
	}

}
