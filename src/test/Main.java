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
	 
	  
	    public static void showTimer() {  
	        TimerTask task = new TimerTask() {  
	            @Override  
	            public void run() {  
	            	mirror.update();
	               
	            }  
	  
	        };  
	  
	        Calendar calendar = Calendar.getInstance();  
	        int year = calendar.get(Calendar.YEAR);  
	        int month = calendar.get(Calendar.MONTH) + 1;  
	        int day = calendar.get(Calendar.DAY_OF_MONTH);  
	        /*** 定制每日00：24：00执行方法 ***/  
	        calendar.set(year, month, day, 00, 00, 00);  
	        Date date = calendar.getTime();  
	        Timer timer = new Timer();  
	        timer.schedule(task, date);  
	    }  
	public static void main(String[] args) {
		// TODO 自动生成的方法存根
		
		System.out.println("starting......");
		System.out.println("loading "+name+" "+version+" BY:"+maker+" "+buliddate);
		showTimer();
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
