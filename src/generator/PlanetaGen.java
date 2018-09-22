package generator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import utils.Pair;

public class PlanetaGen {

	private double length;
	private int processors;
	private int tasks;
	private int fanout;
	private int maxdist;

	private List<List<Double>> timelines;
	private List<GTask> allTasks;
	private List<Pair> dependencies;

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
		dependencies = new ArrayList<>();
	}

	public void generate() {
		for (int p = 0; p < processors; p++) {
			int number_of_tasks = new Random().nextInt(tasks / processors) + 1;
			for (int j = 0; j < number_of_tasks; j++) {
				double time = new Random().nextDouble() * length;
				timelines.get(p).add(time);
			}
			Collections.sort(timelines.get(p));
			for (int i = 0; i < timelines.get(p).size() - 1; i++) {
				GTask aTask = new GTask();
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
				while (retry) {
					int task2_i = new Random().nextInt(allTasks.size());
					GTask task2 = allTasks.get(task2_i);
					if (task2.start >= task1.end || task1.start >= task2.end) {
						double d1 = Math.abs(task2.start - task1.end);
						double d2 = Math.abs(task1.start - task2.end);
						double min = d1;
						if (d2 < min)
							min = d2;
						if (min < length / maxdist)
							continue;
						retry = false;
					}
					if (task1_i < task2_i) {
						Pair pair = new Pair(task1_i, task2_i);
						dependencies.add(pair);
					} else {
						Pair pair = new Pair(task2_i, task1_i);
						dependencies.add(pair);
					}
				}
			}
		}
	}

	public void printDetails() {
		StringBuffer sb = new StringBuffer();
		sb.append("Tasks:\n");
		for (GTask t : allTasks) {
			sb.append(String.format("%.1f-%.1f(%d) ", t.start, t.end, t.cpu));
		}
		sb.append("\nDependencies:\n");
		for (Pair pair : dependencies) {
			sb.append(String.format("%d->%d ", pair.x, pair.y));
		}
		System.out.println(sb.toString());
	}

	public static void main(String[] args) {
		PlanetaGen generator = new PlanetaGen(1000000, 4, 100, 6, 7);
		generator.generate();
		generator.printDetails();
	}

}
