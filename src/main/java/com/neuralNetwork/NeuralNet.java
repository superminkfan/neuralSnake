package com.neuralNetwork;

import java.awt.*;

public class NeuralNet {
	public Stage stages[];

	/**
	 * @param stageSizes
	 *            массив определяющий сколько нейронов в каждом слое, например
	 *            {48,16,16,2}.
	 */
	public NeuralNet(int stageSizes[]) {
		stages = new Stage[stageSizes.length];
		Stage prev = null;
		for (int i = 0; i < stageSizes.length; i++) {
			stages[i] = new Stage(prev, stageSizes[i]);
			prev = stages[i];
		}
	}
	
	/**
	 * Загружает веса / коэффициенты из линейной последовательности массива в нейронную сеть
	 * @param coeffs	массив с коэфициентами от -128 до +127.
	 * никаких трай катсчей нет!
	 * сломается если не дать точный размер массива
	 */
	public void loadCoeffs(byte coeffs[]) {
		int idx = 0;
		for (int s = 1; s < stages.length; s++) {
			for (int i = 0; i < stages[s].coeffs.length; i++) {
				for (int j = 0; j < stages[s].coeffs[0].length; j++) {
					stages[s].coeffs[i][j] = coeffs[idx++];
				}
			}
		}
	}
	
	/**
	 * то же самое что и loadCoeffs(), но заполняет нейронную сеть симметрично
	 * используется только тогда когда стайджи одинаковые
	 * @param coeffs массив с коэфициентами от -128 до +127.
	 */

	public void loadCoeffsSymmetrical(byte coeffs[]) {
		int idx = 0;
		for (int s = 1; s < stages.length; s++) {
			if (stages[s].coeffs.length % 2 == 1) {
				System.err.println("Symmetrical Net without even sized stages. Bad.");
				return;
			}
			for (int i = 0; i < (stages[s].coeffs.length) / 2; i++) {
				for (int j = 0; j < stages[s].coeffs[0].length; j++) {
					stages[s].coeffs[i][j] = coeffs[idx];
					stages[s].coeffs[stages[s].coeffs.length - 1 - i][stages[s].coeffs[0].length - 1 - j] = coeffs[idx++];
				}
			}
		}
	}
	
	/**
	 * вычисление выхода нейронной сети на основании входных данных
	 * 
	 * @param input		входной вектор (значения первой ступени)
	 * @return			выходной вектор
	 */

	public double[] calc(double input[]) {
		for (int i = 0; i < input.length; i++) {
			stages[0].output[i] = input[i];
		}
		for (int i = 1; i < stages.length; i++) {
			stages[i].calc();
		}
		return stages[stages.length - 1].output;
	}


	public static int calcNumberOfCoeffs(int stageSizes[], boolean symmetrical) {
		int sum = 0;
		if (stageSizes.length < 2)
			return 0;
		for (int i = 1; i < stageSizes.length; i++) {
			if (symmetrical)
				sum += (stageSizes[i] * (stageSizes[i - 1] + 1) + 1) / 2;
			else
				sum += stageSizes[i] * (stageSizes[i - 1] + 1);
		}

		return sum;
	}

	public String toString() {
		String k = "";
		for (int s = 1; s < stages.length; s++) {
			k += "\nStage " + s + ": \n" + stages[s].toString();
		}
		return k;
	}


	public void display(Graphics g, float alpha, double w, double h) {
		Graphics2D g2 = (Graphics2D) g;
		int d = 20;

		// synapses:
		for (int s = 1; s < stages.length; s++) {
			int x1 = (s) * (int) (w / (stages.length + 1));
			int x2 = (s + 1) * (int) (w / (stages.length + 1));

			for (int i = 0; i < stages[s].coeffs.length; i++) {
				for (int j = 0; j < stages[s].coeffs[0].length - 1; j++) {
					int c = stages[s].coeffs[i][j];
					if (Math.abs(c) < 48)
						continue;
					g2.setStroke(new BasicStroke(Math.abs(c) * 3 / 129));

					int y1 = (j + 1) * (int) (h / (stages[s - 1].output.length + 1));
					int y2 = (i + 1) * (int) (h / (stages[s].output.length + 1));
					float b = (float) (stages[s - 1].output[j] / Stage.signalMultiplier);
					if (c < 0)
						g.setColor(new Color(b, 0, 0));
					else
						g.setColor(new Color(0, b, 0));
					g2.drawLine(x1, y1, x2, y2);
				}
			}
		}

		// neurons:
		for (int s = 0; s < stages.length; s++) {
			int x = (s + 1) * (int) (w / (stages.length + 1));
			d = (int) (h / (stages[s].output.length + 7));
			for (int i = 0; i < stages[s].output.length; i++) {
				int y = (i + 1) * (int) (h / (stages[s].output.length + 1));

				float output = (float) (stages[s].output[i] / Stage.signalMultiplier * .8 + .2);
				g.setColor(new Color(Color.HSBtoRGB(.6f, 1, output)));
				g.fillOval(x - d / 2, y - d / 2, d, d);
			}
		}

	}
}
