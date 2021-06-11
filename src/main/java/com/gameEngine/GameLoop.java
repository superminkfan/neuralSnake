package com.gameEngine;


import com.genetics.DNA;
import com.helpers.KeyboardListener;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;

public class GameLoop extends JComponent {
	// главная частота обновления
	public static final long UPDATEPERIOD = 8;
	public double per = UPDATEPERIOD;

	// переменные:
	public static final int globalCircleRadius = 20;
	public static final int numSnakes = 8;
	public static final int numNibbles = 4;

	// параметры генетического алгоритма
	public static double mutationrate = .02;
	public double currentGeneration = 0;

	//инициализация мира и змей:
	public World world = new World();
	public LinkedList<Snake> snakes = new LinkedList<Snake>();
	public LinkedList<Snake> backupSnakes = new LinkedList<Snake>();

	// лучшая:
	public DNA bestDna = null;
	public double bestscore = 0;

	// статистика:
	public LinkedList<Double> fitnessTimeline = new LinkedList<Double>();
	public double currentMaxFitness = 0;

	// контроль режима:
	public boolean singleSnakeModeActive = false;
	public boolean displayStatisticsActive = false;
	public boolean simulationPaused = false;

	/**
	 * вообще графика должна быть раздельно от этого но мне было лень
	 */
	public GameLoop(KeyboardListener keyb) {
		world.height = 200;
		world.width = 300;
		new Thread(new Runnable() {
			private long simulationLastMillis;
			private long statisticsLastMillis;

			public void run() {
				simulationLastMillis = System.currentTimeMillis() + 100; //ждем пока настроится граика
				statisticsLastMillis = 0;
				while (true) {
					if (System.currentTimeMillis() - simulationLastMillis > UPDATEPERIOD) {
						synchronized (snakes) {
							long currentTime = System.currentTimeMillis();
							// управление
							char keyCode = (char) keyb.getKey();
							switch (keyCode) {
							case ' ': // пробел
								if (!singleSnakeModeActive) {
									singleSnakeModeActive = true;
									displayStatisticsActive = false;
									backupSnakes.clear();
									backupSnakes.addAll(snakes);
									snakes.clear();
									snakes.add(new Snake(bestDna, world));
								}
								break;
							case 'A': // a = pause
								simulationPaused = true;
								break;
							case 'B': // b = resume
								simulationPaused = false;
								break;
							case 'C': // c = show stats
								displayStatisticsActive = true;
								break;
							case 'D': // d = hide stats
								displayStatisticsActive = false;
								break;
							}
							// инициализация первого покаления
							if (snakes.isEmpty()) {
								firstGeneration(numSnakes);
								world.newNibble(numNibbles);
							}
							// Вычисления:
							if (!simulationPaused) {
								int deadCount = 0;
								world.update(getWidth(), getHeight());
								synchronized (fitnessTimeline) {
									if (world.clock - statisticsLastMillis > 1000 && !singleSnakeModeActive) {
										fitnessTimeline.addLast(currentMaxFitness);
										currentMaxFitness = 0;
										if (fitnessTimeline.size() >= world.width / 2) {
											fitnessTimeline.removeFirst();
										}
										statisticsLastMillis = world.clock;
									}
								}
								for (Snake s : snakes) {
									if (!s.update(world)) {
										deadCount++;
									}
									if (s.getFitness() > currentMaxFitness)
										currentMaxFitness = s.getFitness();
									if (s.getFitness() > bestscore) {
										bestscore = s.getFitness();
										bestDna = s.dna;
									}
								}
								if (deadCount > 0 && singleSnakeModeActive) {
									singleSnakeModeActive = false;
									snakes.clear();
									snakes.addAll(backupSnakes);

								} else {
									// новые змеи
									for (int i = 0; i < deadCount; i++) {
										newSnake();
										currentGeneration += 1 / (double) numSnakes;
									}
								}
								Iterator<Snake> it = snakes.iterator();
								while (it.hasNext()) {
									Snake s = it.next();
									if (s.deathFade <= 0) {
										it.remove();
									}
								}
							} else {
								//вывести статус
								snakes.get(0).brain(world);
							}

							repaint();
							per = System.currentTimeMillis() - currentTime;
							simulationLastMillis += UPDATEPERIOD;
						}
					}
				}
			}
		}).start();
	}


	public void firstGeneration(int n) {
		snakes.clear();
		for (int i = 0; i < n; i++) {
			snakes.add(new Snake(null, world));
		}
		world.reset();
	}

	/**
	 * создания брачного бассейна для змей
	 * 
	 * @return брачный бассейн как лист
	 */
	public ArrayList<Snake> makeMatingpool() {
		ArrayList<Snake> matingpool = new ArrayList<Snake>();
		double maxscore = 0;
		for (Snake s : snakes) {
			if (s.getFitness() > maxscore) {
				maxscore = s.getFitness();
			}
		}
		for (Snake s : snakes) {
			int amount = (int) (s.getFitness() * 100 / maxscore);
			for (int i = 0; i < amount; i++) {
				matingpool.add(s);
			}
		}
		return matingpool;
	}

	/**
	 * создаёт новую змею используя генетический алгоритм и добавляет его в список
	 */
	public void newSnake() {
		mutationrate = 10 / currentMaxFitness;
		ArrayList<Snake> matingpool = makeMatingpool();
		int idx1 = (int) (Math.random() * matingpool.size());
		int idx2 = (int) (Math.random() * matingpool.size());
		DNA parentA = matingpool.get(idx1).dna;
		DNA parentB = matingpool.get(idx2).dna;
		snakes.add(new Snake(parentA.crossoverBytewise(parentB, mutationrate), world));
		//snakes.add(new Snake(parentA.crossover(parentB, mutationrate), world));
	}

	/**
	 * показ статистики
	 */
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		// задний план:
		g.setColor(Color.black);
		g.fillRect(0, 0, getWidth(), getHeight());

		// статы:
		if (displayStatisticsActive) {
			g.setColor(Color.DARK_GRAY);
			g.setFont(new Font("Ареал", 0, 64));
			g.drawString("t = " + Long.toString(world.clock / 1000), 20, 105);

			g.drawString("поколение = " + Integer.toString((int) currentGeneration), 20, 205);
			g.setFont(new Font("Ареал", 0, 32));
			g.drawString("Процент мутации.: " + String.format("%1$,.3f", mutationrate), 20, 305);
			g.drawString("Максимальная пригодность: " + Integer.toString((int) currentMaxFitness), 20, 355);

			// таймлайн
			synchronized (fitnessTimeline) {
				if (!fitnessTimeline.isEmpty()) {
					double last = fitnessTimeline.getFirst();
					int x = 0;
					double limit = getHeight();
					if (limit < bestscore)
						limit = bestscore;
					for (Double d : fitnessTimeline) {
						g.setColor(new Color(0, 1, 0, .5f));
						g.drawLine(x, (int) (getHeight() - getHeight() * last / limit), x + 2, (int) (getHeight() - getHeight() * d / limit));
						last = d;
						x += 2;
					}
				}
			}
		}
		// нейронная сеть:
		if (singleSnakeModeActive) {
			snakes.getFirst().brainNet.display(g, 0, world.width, world.height);
		}
		// змеи:
		synchronized (snakes) {
			for (Snake s : snakes)
				s.draw(g);
			world.draw(g);
		}
	}

}
