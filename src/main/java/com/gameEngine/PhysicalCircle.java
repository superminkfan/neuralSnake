package com.gameEngine;

public class PhysicalCircle {
	/**
	 * Класс для реализации 2д мячика / круговой объекс; позиция, скорость,
	 * радиус, таймер для основных целей t, базовые физические процессы
	 */
	public double x;
	public double y;
	public double vx = 0;
	public double vy = 0;
	public double rad;
	public long t = 0;
	

	/**
	 * @param x		x поозиция
	 * @param y		y позиция
	 * @param rad	радиус круга
	 */

	public PhysicalCircle(double x, double y, double rad) {
		this.x = x;
		this.y = y;
		this.rad = rad;
	}



	
	public void collideWall(double xmin, double ymin, double xmax, double ymax) {
		if (x - rad < xmin) {
			x = xmin + rad;
			vx = -vx * .9;
		}
		if (x + rad > xmax) {
			x = xmax - rad;
			vx = -vx * .9;
		}
		if (y - rad < ymin) {
			y = ymin + rad;
			vy = -vy * .9;
		}
		if (y + rad > ymax) {
			y = ymax - rad;
			vy = -vy * .9;
		}
	}

	public void constrainSpeed(double maxspeed, double fadeout) {
		vx *= fadeout;
		vy *= fadeout;
		if (Math.abs(vx) < .001)
			vx = 0;
		if (Math.abs(vy) < .001)
			vy = 0;
		if (vx > maxspeed)
			vx = maxspeed;
		if (vx < -maxspeed)
			vx = -maxspeed;
		if (vy > maxspeed)
			vy = maxspeed;
		if (vy < -maxspeed)
			vy = -maxspeed;
	}

	public void updatePosition() {
		x += vx;
		y += vy;
	}
	


	public void collideStatic(PhysicalCircle o) {
		if (this == o)
			return;
		double s = this.rad + o.rad;
		double d = Math.sqrt((this.x - o.x) * (this.x - o.x) + (this.y - o.y) * (this.y - o.y));
		double a = Math.atan2(this.y - o.y, this.x - o.x);

		if (d < s) {
			this.x = o.x + s * Math.cos(a);
			this.y = o.y + s * Math.sin(a);
		}

	}
	

	public void collideBouncy(PhysicalCircle o, double speed) {
		if (this == o)
			return;
		double s = this.rad + o.rad;
		double d = Math.sqrt((this.x - o.x) * (this.x - o.x) + (this.y - o.y) * (this.y - o.y));
		double a = Math.atan2(this.y - o.y, this.x - o.x);

		if (d < s) {
			this.x = o.x + s * Math.cos(a);
			this.y = o.y + s * Math.sin(a);
			this.vx -= (o.x - this.x) * 2 / d * speed / 5;
			this.vy -= (o.y - this.y) * 2 / d * speed / 5;
		}

	}

	public void followBouncy(PhysicalCircle o) {
		if (this == o)
			return;
		double s = this.rad + o.rad;
		double a = Math.atan2(this.y - o.y, this.x - o.x);
		this.vx += (o.x + s * Math.cos(a) - this.x) / s / 32;
		this.vy += (o.y + s * Math.sin(a) - this.y) / s / 32;
		this.x += (o.x + s * Math.cos(a) - this.x) / s * 24 + o.vx * .24;
		this.y += (o.y + s * Math.sin(a) - this.y) / s * 24 + o.vy * .24;
	}

	public void followStatic(PhysicalCircle o) {
		if (this == o)
			return;
		double s = this.rad + o.rad;
		double a = Math.atan2(this.y - o.y, this.x - o.x);
		this.x = (o.x + s * Math.cos(a));
		this.y = (o.y + s * Math.sin(a));
	}

	public boolean isColliding(PhysicalCircle o, double thresholdDistance) {
		double d = Math.sqrt((this.x - o.x) * (this.x - o.x) + (this.y - o.y) * (this.y - o.y));
		double s = this.rad + o.rad;
		return d < s + thresholdDistance;
	}

	public double getAbsoluteVelocity() {
		return Math.sqrt(vx * vx + vy * vy);
	}

	public double getDistanceTo(PhysicalCircle o) {
		return Math.sqrt((this.x - o.x) * (this.x - o.x) + (this.y - o.y) * (this.y - o.y)) - (this.rad - o.rad) / 2;
	}

	public double getAngleTo(PhysicalCircle o) {
		return Math.atan2(o.y - this.y, o.x - this.x);
	}
}
