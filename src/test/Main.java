package test;

import java.util.Calendar;
import java.util.Date;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;

import updater.mirror;

public class Main {
	public static String name="minecraft mirror";
	public static String version="0.0.1";
	public static String maker="麻花a";
	public static String buliddate="2017,11,4";
	public static Boolean noexit=true;
	static Scanner s = new Scanner(System.in);
	 
	  

	public static void main(String[] args) {
		// TODO 自动生成的方法存根
		
		System.out.println("starting......");
		System.out.println("loading "+name+" "+version+" BY:"+maker+" "+buliddate);
		
		while (noexit) {
			command(input());
			
			
			
				
				
			
			
			
		}
		

	}
	public static String input() {
		
		return s.nextLine();
	}
	public static void command(String args) {
		if(args.equals("update")) {
			mirror.update();
			return;
			
		
			
		}
		if(args.equals("quit")) {
			noexit=false;
			return;
			
		
			
		}
		if(args.equals("help")) {
			//noexit=false;
			System.out.println("quit 退出程序 \nhelp 查看帮助列表");
			return;
			
		
			
		}
		System.out.println("未知指令，请使用help查看指令列表");
		
	}

}
