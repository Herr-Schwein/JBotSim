package examples.fancy.parkcleaning;

import jbotsim.Node;

public class Garbage extends Node{
	public Garbage(){
		disableWireless();
		setIcon("gmgarbage.png");
		setSize(12);
	}
}
