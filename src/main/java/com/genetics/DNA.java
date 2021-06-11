package com.genetics;

import java.util.Arrays;
import java.util.Random;

public class DNA {
	/**
	 * Класс моделирования ДНК, мутаций и перемешевания
	 */
	public Random random = new Random();
	public byte data[];
	
	public DNA(boolean random, int size){
		data = new byte[size];
		
		for (int i = 0; i < data.length; i++){
			data[i] = random?(byte)Math.floor(Math.random()*256d):0;
		}
	}
	/**
	 * Функция кроссовера, которая объединяет эту ДНК с другим объектом ДНК.
	 * Процесс выполняется побайтно, и к каждому значению байта добавляется гауссовский шум
	 * Биты переворачиваются в зависимости от вероятности мутации
	 */
	public DNA crossoverNoise(DNA other, double mutationprob){
		DNA newdna = new DNA(false, data.length);
		int numswaps = data.length/10; 
		int swaps[] = new int[numswaps+1];
		for (int i = 0; i < swaps.length-1; i++){
			swaps[i] = (int)Math.floor(Math.random()*data.length);
		}
		swaps[numswaps] = data.length;  //save last
		Arrays.sort(swaps);
		int swapidx = 0;
		boolean that = true;
		for (int i = 0; i < data.length; i++){
			if (i >= swaps[swapidx]){
				swapidx++;
				that = !that;
			}
			byte d = 0;
			if (that){
				d = this.data[i];
			}
			else {
				d = other.data[i];
			}
			d += (byte)(random.nextGaussian()*mutationprob*256);
			newdna.data[i] = d;
		}
		return newdna;
	}
	/**
	 * Метод мутации по гауссу
	 */
	public void mutateNoise(double prob, double mag){
		for (int i = 0; i < data.length; i++){
			if (Math.random() < prob) data[i] += (byte)(random.nextGaussian()*mag*256);
		}
	}
	/**
	 * Функция кроссовера, которая объединяет эту ДНК с другим объектом ДНК.
	 * Процесс делается побитовым
	 * Биты переворачиваются в зависимости от вероятности мутации
	 */
	public DNA crossover(DNA other, double mutationprob){
		DNA newdna = new DNA(false, data.length);
		int numswaps = data.length/8; 
		int swaps[] = new int[numswaps+1];
		for (int i = 0; i < swaps.length-1; i++){
			swaps[i] = (int)Math.floor(Math.random()*8*data.length);
		}
		swaps[numswaps] = 8*data.length;  //save last
		Arrays.sort(swaps);
		int swapidx = 0;
		boolean that = true;
		for (int i = 0; i < 8*data.length; i++){
			if (i >= swaps[swapidx]){
				swapidx++;
				that = !that;
			}
			int bit = 0;
			if (that){
				bit = ((this.data[i/8] >> (i%8)) & 1);
			}
			else {
				bit = ((other.data[i/8] >> (i%8)) & 1);
			}
			if (Math.random() < mutationprob) bit = 1-bit;
			newdna.data[i/8] |= (bit << (i%8));
		}
		return newdna;
	}
	/**
	 * Функция кроссовера, которая объединяет эту ДНК с другим объектом ДНК.
	 * Процесс выполняется только побайтно, поэтому добавляется меньше шума
	 * Биты переворачиваются в зависимости от вероятности мутации
	 */
	public DNA crossoverBytewise(DNA other, double mutationprob){
		DNA newdna = new DNA(false, data.length);
		int numswaps = data.length/8; 
		int swaps[] = new int[numswaps+1];
		for (int i = 0; i < swaps.length-1; i++){
			swaps[i] = 8*(int)Math.floor(Math.random()*data.length);
		}
		swaps[numswaps] = 8*data.length;  //save last
		Arrays.sort(swaps);
		int swapidx = 0;
		boolean that = true;
		for (int i = 0; i < 8*data.length; i++){
			if (i >= swaps[swapidx]){
				swapidx++;
				that = !that;
			}
			int bit = 0;
			if (that){
				bit = ((this.data[i/8] >> (i%8)) & 1);
			}
			else {
				bit = ((other.data[i/8] >> (i%8)) & 1);
			}
			if (Math.random() < mutationprob) bit = 1-bit;
			newdna.data[i/8] |= (bit << (i%8));
		}
		return newdna;
	}
}