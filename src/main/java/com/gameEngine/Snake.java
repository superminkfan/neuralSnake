package com.gameEngine;



import com.genetics.DNA;
import com.helpers.DoubleMath;
import com.neuralNetwork.NeuralNet;
import com.neuralNetwork.Stage;

import java.awt.*;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class Snake {

	// константы движения:
	public static final double maximumForwardSpeed = 5;
	public static final double maximumAngularSpeed = Math.PI / 32d;
	public static final double wallCollisionThreshold = 4;

	// видовые константы:
	public static final double maximumSightDistance = 600;
	public static final double fieldOfView = Math.PI * 2 / 3;

	// константы нейронной сети:
	public static final int FOVDIVISIONS = 8;
	public static final int FIRSTSTAGESIZE = FOVDIVISIONS * 2 * 3;
	public static final int stageSizes[] = new int[] { FIRSTSTAGESIZE, 16, 16, 2 };
	public static final boolean isNNSymmetric = false;

	// константы подсчёта:
	public static final double nibblebonus = 20;
	public static final int healthbonus = 10; // добовляется каждый рз как змея ест
	public static final double healthdecrement = .02; // декремент каждый круг

	// разное:
	public final boolean displayCuteEyes = true;
	public final boolean snakeInertia = false;

	// основные атрибуты змей:
	public ArrayList<PhysicalCircle> snakeSegments = new ArrayList<PhysicalCircle>(100);
	public DNA dna;
	public NeuralNet brainNet;
	public double age = 0;
	public double angle;
	public double score;
	public boolean isDead;
	public float hue;
	public double deathFade = 180;
	public double health;

	/**
	 * Инициализация новой змеи с данным ДНК
	 * 
	 * @param dna
	 *            если ноль, то генерируется случайное ДНК
	 * @param world
	 *            чтобы узнать точку рождения
	 */

	public Snake(DNA dna, World world) {
		double x = Math.random() * (world.width - 2 * wallCollisionThreshold - 2 * GameLoop.globalCircleRadius) + wallCollisionThreshold
				+ GameLoop.globalCircleRadius;
		double y = Math.random() * (world.height - 2 * wallCollisionThreshold - 2 * GameLoop.globalCircleRadius) + wallCollisionThreshold
				+ GameLoop.globalCircleRadius;

		int dnalength = NeuralNet.calcNumberOfCoeffs(stageSizes, isNNSymmetric) + 1;
		if (dna == null) {
			this.dna = new DNA(true, dnalength);
		} else {
			this.dna = dna;
		}
		snakeSegments.clear();
		for (int i = 0; i < 1; i++) {
			snakeSegments.add(new PhysicalCircle(x, y, GameLoop.globalCircleRadius));
		}
		this.angle = Math.atan2(world.height / 2 - y, world.width / 2 - x);
		// установка мозгов:
		brainNet = new NeuralNet(stageSizes);
		reloadFromDNA();
		score = 0;
		deathFade = 180;
		isDead = false;
		health = healthbonus * 3 / 2;
		age = 0;
	}


	public void reloadFromDNA() {
		if (isNNSymmetric)
			brainNet.loadCoeffsSymmetrical(this.dna.data);
		else
			brainNet.loadCoeffs(this.dna.data);
		this.hue = (float) this.dna.data[this.dna.data.length - 1] / 256f;
	}


	public boolean update(World world) {
		if (isDead) {
			deathFade -= .6;
			return true;
		}
		age += .1;
		double slowdown = 49d / (48d + snakeSegments.size());
		PhysicalCircle head = snakeSegments.get(0);

		double angleIncrement = brain(world);

		angle += slowdown * angleIncrement;
		angle = DoubleMath.doubleModulo(angle, Math.PI * 2);

		// коллизия со стеной
		if (head.x - head.rad < wallCollisionThreshold) {
			score /= 2;
			isDead = true;
		}
		if (head.x + head.rad > world.width - wallCollisionThreshold) {
			score /= 2;
			isDead = true;
		}
		if (head.y - head.rad < wallCollisionThreshold) {
			score /= 2;
			isDead = true;
		}
		if (head.y + head.rad > world.height - wallCollisionThreshold) {
			score /= 2;
			isDead = true;
		}
		//основные движения:
		head.vx = maximumForwardSpeed * slowdown * Math.cos(angle);
		head.vy = maximumForwardSpeed * slowdown * Math.sin(angle);

		PhysicalCircle previous = head;
		for (int i = 0; i < snakeSegments.size(); i++) {
			PhysicalCircle c = snakeSegments.get(i);
			if (snakeInertia){
				c.followBouncy(previous);
			} else {
				c.followStatic(previous);
			}
			
			c.updatePosition();
			for (int j = 0; j < i; j++) {
				c.collideStatic(snakeSegments.get(j));
				//c.collideBouncy(snakeSegments.get(j),maximumForwardSpeed);
			}
			previous = c;
			if (i > 1 && head.isColliding(c, 0)) {
				isDead = true;
				score /= 2;
				break;
			}
		}

		LinkedList<PhysicalCircle> nibblesToRemove = new LinkedList<PhysicalCircle>();
		int nibbleEatCount = 0;
		for (PhysicalCircle nibble : world.getNibbles()) {
			if (head.isColliding(nibble, -10)) {
				score += world.calcValue(nibble);
				snakeSegments.add(new PhysicalCircle(snakeSegments.get(snakeSegments.size() - 1).x, snakeSegments.get(snakeSegments.size() - 1).y, nibble.rad));
				nibblesToRemove.add(nibble);
				nibbleEatCount++;
			}
		}
		score += nibbleEatCount * nibblebonus;
		world.newNibble(nibbleEatCount);
		world.removeNibbles(nibblesToRemove);

		// health / hunger:
		health += nibbleEatCount * healthbonus;
		if (health > 3 * healthbonus) // saturate
			health = 3 * healthbonus;
		health -= healthdecrement;
		if (health <= 0) {
			isDead = true;
			score /= 2;
		}
		return !isDead;
	}


	public double getFitness() {
		return score + health / 4;
	}


	public class Thing {
		public double distance = maximumSightDistance;
		public int type = 0;
		// Wall = 0;
		// Snake = 1;
		// Nibble = 2;
	}

	/**
	 * Main calculation
     */
	public double brain(World world) {
		// init input vector:
		Thing input[] = new Thing[FOVDIVISIONS * 2];
		for (int i = 0; i < FOVDIVISIONS * 2; i++)
			input[i] = new Thing();
		// nibbles:
		input = updateVisualInput(input, world.getNibbles(), 2);
		// snake:
		input = updateVisualInput(input, snakeSegments, 1);
		// walls:

		int step = (int) (maximumSightDistance * Math.sin(fieldOfView / (FOVDIVISIONS * 1d))) / 20;
		LinkedList<PhysicalCircle> walls = new LinkedList<PhysicalCircle>();
		for (int x = 0; x < world.width; x += step) {
			walls.add(new PhysicalCircle(x, 0, 1));
			walls.add(new PhysicalCircle(x, world.height, 1));
		}
		for (int y = 0; y < world.height; y += step) {
			walls.add(new PhysicalCircle(0, y, 1));
			walls.add(new PhysicalCircle(world.width, y, 1));
		}
		input = updateVisualInput(input, walls, 0);


		double stageA[] = new double[FIRSTSTAGESIZE]; // zeros initialized ;)
		if (isNNSymmetric) {
			for (int i = 0; i < FOVDIVISIONS; i++) {
				stageA[input[i].type * FOVDIVISIONS + i] = Stage.signalMultiplier * (maximumSightDistance - input[i].distance) / maximumSightDistance;
				stageA[FIRSTSTAGESIZE - 1 - (input[i + FOVDIVISIONS].type * FOVDIVISIONS + i)] = Stage.signalMultiplier
						* (maximumSightDistance - input[i + FOVDIVISIONS].distance) / maximumSightDistance;
			}
		} else {
			for (int i = 0; i < FOVDIVISIONS; i++) {
				stageA[input[i].type * FOVDIVISIONS * 2 + i] = Stage.signalMultiplier * (maximumSightDistance - input[i].distance) / maximumSightDistance;
				stageA[input[i + FOVDIVISIONS].type * FOVDIVISIONS * 2 + FOVDIVISIONS * 2 - 1 - i] = Stage.signalMultiplier
						* (maximumSightDistance - input[i + FOVDIVISIONS].distance) / maximumSightDistance;
			}
		}
		double output[] = brainNet.calc(stageA);
		double delta = output[0] - output[1];
		double angleIncrement = 10 * maximumAngularSpeed / Stage.signalMultiplier * delta;
		if (angleIncrement > maximumAngularSpeed)
			angleIncrement = maximumAngularSpeed;
		if (angleIncrement < -maximumAngularSpeed)
			angleIncrement = -maximumAngularSpeed;
		return angleIncrement;
	}


	private Thing[] updateVisualInput(Thing input[], List<PhysicalCircle> objects, int type) {
		PhysicalCircle head = snakeSegments.get(0);
		for (PhysicalCircle n : objects) {
			if (head == n)
				continue;
			double a = DoubleMath.signedDoubleModulo(head.getAngleTo(n) - angle, Math.PI * 2);
			double d = head.getDistanceTo(n);
			if (a >= 0 && a < fieldOfView) {
				if (d < input[(int) (a * FOVDIVISIONS / fieldOfView)].distance) {
					input[(int) (a * FOVDIVISIONS / fieldOfView)].distance = d;
					input[(int) (a * FOVDIVISIONS / fieldOfView)].type = type;
				}
			} else if (a <= 0 && -a < fieldOfView) {
				if (d < input[(int) (-a * FOVDIVISIONS / fieldOfView) + FOVDIVISIONS].distance) {
					input[(int) (-a * FOVDIVISIONS / fieldOfView) + FOVDIVISIONS].distance = d;
					input[(int) (-a * FOVDIVISIONS / fieldOfView) + FOVDIVISIONS].type = type;
				}
			}
		}
		return input;
	}


	public void draw(Graphics g) {
		// тело змеи
		int alpha = (int) deathFade;
		for (int i = 0; i < snakeSegments.size(); i++) {
			//Color c = new Color(Color.HSBtoRGB(hue, 1 - (float) i / ((float) snakeSegments.size() + 1f), 1));
			Color c = new Color(Color.HSBtoRGB(hue, 1 / ((float) snakeSegments.size() + 1f), 1));
			g.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), alpha));
			PhysicalCircle p = snakeSegments.get(i);
			g.fillOval((int) (p.x - p.rad), (int) (p.y - p.rad), (int) (2 * p.rad + 1), (int) (2 * p.rad + 1));
		}

		if (displayCuteEyes) {

			PhysicalCircle p = snakeSegments.get(0); // get head
			double dist = p.rad / 2.3;
			double size = p.rad / 3.5;
			g.setColor(new Color(255, 255, 255, alpha));
			g.fillOval((int) (p.x + p.vy * dist / p.getAbsoluteVelocity() - size), (int) (p.y - p.vx * dist / p.getAbsoluteVelocity() - size),
					(int) (size * 2 + 1), (int) (size * 2 + 1));
			g.fillOval((int) (p.x - p.vy * dist / p.getAbsoluteVelocity() - size), (int) (p.y + p.vx * dist / p.getAbsoluteVelocity() - size),
					(int) (size * 2 + 1), (int) (size * 2 + 1));
			size = p.rad / 6;
			g.setColor(new Color(0, 0, 0, alpha));
			g.fillOval((int) (p.x + p.vy * dist / p.getAbsoluteVelocity() - size), (int) (p.y - p.vx * dist / p.getAbsoluteVelocity() - size),
					(int) (size * 2 + 1), (int) (size * 2 + 1));
			g.fillOval((int) (p.x - p.vy * dist / p.getAbsoluteVelocity() - size), (int) (p.y + p.vx * dist / p.getAbsoluteVelocity() - size),
					(int) (size * 2 + 1), (int) (size * 2 + 1));
		}
	}
}
