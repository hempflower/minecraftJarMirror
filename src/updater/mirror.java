package updater;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;

import com.google.gson.Gson;

public class mirror {
	public static void update() {
		File ver=new File(getAppPath(mirror.class)+"\\mcdl\\versions.json");
		if(ver.exists() ){
			ver.delete();
		}
		File ley =new File(getAppPath(mirror.class)+"\\mcdl\\asstes\\"+"legacy.json");
		if(ley.exists()) {
			ley.delete();
			
		}
		try {
			downLoadFromUrl("https://launchermeta.mojang.com/mc/assets/legacy/c0fd82e8ce9fbc93119e40d96d5a4e62cfa3f729/legacy.json","legacy.json",getAppPath(mirror.class)+"\\mcdl\\asstes\\indexes\\");
		} catch (IOException e1) {
			// TODO 自动生成的 catch 块
			e1.printStackTrace();
		}
		downversions();
		String versionsjson=readin(getAppPath(mirror.class)+"\\mcdl\\versions.json");
		Gson gson = new Gson();
		 list li=gson.fromJson(versionsjson, list.class);
		 for(int x=0;x<li.versions.length;x++) {
			 File file=new File(getAppPath(mirror.class)+"\\mcdl\\"+li.versions[x].id);
		        if(!file.exists()){  
		            file.mkdir();  
		        } 
		     try {
				downLoadFromUrl(li.versions[x].url,li.versions[x].id+".json",getAppPath(mirror.class)+"\\mcdl\\"+li.versions[x].id);
			} catch (IOException e) {
				// TODO 自动生成的 catch 块
				e.printStackTrace();
			}
		     String verjson=readin(getAppPath(mirror.class)+"\\mcdl\\"+li.versions[x].id+"\\"+li.versions[x].id+".json");
		     fomat fo=gson.fromJson(verjson, fomat.class);
		     
		     try {
				downLoadFromUrl(fo.downloads.client.url,li.versions[x].id+".jar",getAppPath(mirror.class)+"\\mcdl\\"+li.versions[x].id);
			} catch (IOException e) {
				// TODO 自动生成的 catch 块
				e.printStackTrace();
			}
		     if(!fo.assetIndex.url.equals("https://launchermeta.mojang.com/mc/assets/legacy/c0fd82e8ce9fbc93119e40d96d5a4e62cfa3f729/legacy.json")){
		    	 try {
		    	 //System.out.println(li.versions[x].id);
		    		 downLoadFromUrl(fo.assetIndex.url,fo.assetIndex.id+".json",getAppPath(mirror.class)+"\\mcdl\\asstes\\indexes\\");
		     	} catch (IOException e) {
		     		// TODO 自动生成的 catch 块
		     		e.printStackTrace();
				}
		    	 
		    }
		    
		    // Object obj=jstoob(getAppPath(mirror.class)+"\\mcdl\\asstes\\indexes"+fo.assetIndex.id+".json",Object.class);
		     
		     //assteslist al=gson.fromJson(readin(getAppPath(mirror.class)+"\\mcdl\\asstes\\indexes\\"+fo.assetIndex.id+".json"), assteslist.class);
		     //asstesobj aobj=gson.fromJson(readin(getAppPath(mirror.class)+"\\mcdl\\asstes\\indexes\\"+fo.assetIndex.id+".json"), asstesobj.class);
		     //for(int y=0;y<al.objects.length;y++){
		    //	 if(fo.assetIndex.id.equals("legacy")) {
		    //		 
		    //		 try {
		    //			 downLoadFromUrl("http://resources.download.minecraft.net/"+aobj.objects[y].hash.substring(0, 2)+"/"+aobj.objects[x].hash,getAppPath(mirror.class)+"\\mcdl\\asstes\\virtual\\"+al.objects[y].substring(0,al.objects[y].lastIndexOf("\\") ),al.objects[y].substring(al.objects[y].lastIndexOf("\\"))+"\\");
		    //		 } catch (IOException e) {
		    			 // TODO 自动生成的 catch 块
		    //			 e.printStackTrace();
		    //		 }
		    //	 }else {
			 //   	 try {
			//			downLoadFromUrl("http://resources.download.minecraft.net/"+aobj.objects[y].hash.substring(0, 2)+"/"+aobj.objects[x].hash,aobj.objects[y].hash,getAppPath(mirror.class)+"\\mcdl\\asstes\\objects\\"+aobj.objects[y].hash.substring(0, 2)+aobj.objects[y].hash);
			//		} catch (IOException e) {
			//			// TODO 自动生成的 catch 块
			//			e.printStackTrace();
			//		}
			    	 
			//     }
		     //}
		     //
		 
		     //x=null;
		     //file=null;
		     //verjson=null;
		     
		     
		     
			 

			 
		 }
		 
		
		
	}
	public static void downversions() {
		try {
			//File directory = new File("");
			downLoadFromUrl("https://launchermeta.mojang.com/mc/game/version_manifest.json","versions.json",getAppPath(mirror.class)+"\\mcdl");
			System.out.println("downloading versions succeed!");
		} catch (IOException e) {
			// TODO 自动生成的 catch 块
			e.printStackTrace();
		}
		
		
	}

    public static void  downLoadFromUrl(String urlStr,String fileName,String savePath) throws IOException{  
        URL url = new URL(urlStr);    
        HttpURLConnection conn = (HttpURLConnection)url.openConnection();    
                //设置超时间为10秒  
        conn.setConnectTimeout(10*1000);  
        //防止屏蔽程序抓取而返回403错误  
        conn.setRequestProperty("User-Agent", "Mozilla/4.0 (compatible; MSIE 5.0; Windows NT; DigExt)");  
  
        //得到输入流  
        InputStream inputStream = conn.getInputStream();    
        //获取自己数组  
        byte[] getData = readInputStream(inputStream);      
  
        //文件保存位置  
        File saveDir = new File(savePath);  
        if(!saveDir.exists()){  
            saveDir.mkdir();  
        }  
        
        File file = new File(saveDir+File.separator+fileName);   
        if(file.exists()) {
        	return;
        	
        }
        FileOutputStream fos = new FileOutputStream(file);       
        fos.write(getData);   
        if(fos!=null){  
            fos.close();    
        }  
        if(inputStream!=null){  
            inputStream.close();  
        }  
  
  
        System.out.println("info:"+url+" 被成功的保存！");   
  
    }  
    public static  byte[] readInputStream(InputStream inputStream) throws IOException {    
        byte[] buffer = new byte[1024];    
        int len = 0;    
        ByteArrayOutputStream bos = new ByteArrayOutputStream();    
        while((len = inputStream.read(buffer)) != -1) {    
            bos.write(buffer, 0, len);    
        }    
        bos.close();    
        return bos.toByteArray();    
    }    
    public static String readin(String fileName) {  
        String encoding = "ISO-8859-1";  
        File file = new File(fileName);  
        Long filelength = file.length();  
        byte[] filecontent = new byte[filelength.intValue()];  
        try {  
            FileInputStream in = new FileInputStream(file);  
            in.read(filecontent);  
            in.close();  
        } catch (FileNotFoundException e) {  
            e.printStackTrace();  
        } catch (IOException e) {  
            e.printStackTrace();  
        }  
        try {  
            return new String(filecontent, encoding);  
        } catch (UnsupportedEncodingException e) {  
            System.err.println("The OS does not support " + encoding);  
            e.printStackTrace();  
            return null;  
        }  
    }  
    public static String getAppPath(Class<mirror> cls){
        //检查用户传入的参数是否为空
        if(cls==null) 
         throw new java.lang.IllegalArgumentException("参数不能为空！");
        ClassLoader loader=cls.getClassLoader();
        //获得类的全名，包括包名
        String clsName=cls.getName()+".class";
        //获得传入参数所在的包
        Package pack=cls.getPackage();
        String path="";
        //如果不是匿名包，将包名转化为路径
        if(pack!=null){
            String packName=pack.getName();
           //此处简单判定是否是Java基础类库，防止用户传入JDK内置的类库
           if(packName.startsWith("java.")||packName.startsWith("javax.")) 
              throw new java.lang.IllegalArgumentException("不要传送系统类！");
            //在类的名称中，去掉包名的部分，获得类的文件名
            clsName=clsName.substring(packName.length()+1);
            //判定包名是否是简单包名，如果是，则直接将包名转换为路径，
            if(packName.indexOf(".")<0) path=packName+"/";
            else{//否则按照包名的组成部分，将包名转换为路径
                int start=0,end=0;
                end=packName.indexOf(".");
                while(end!=-1){
                    path=path+packName.substring(start,end)+"/";
                    start=end+1;
                    end=packName.indexOf(".",start);
                }
                path=path+packName.substring(start)+"/";
            }
        }
        //调用ClassLoader的getResource方法，传入包含路径信息的类文件名
        java.net.URL url =loader.getResource(path+clsName);
        //从URL对象中获取路径信息
        String realPath=url.getPath();
        //去掉路径信息中的协议名"file:"
        int pos=realPath.indexOf("file:");
        if(pos>-1) realPath=realPath.substring(pos+5);
        //去掉路径信息最后包含类文件信息的部分，得到类所在的路径
        pos=realPath.indexOf(path+clsName);
        realPath=realPath.substring(0,pos-1);
        //如果类文件被打包到JAR等文件中时，去掉对应的JAR等打包文件名
        if(realPath.endsWith("!"))
            realPath=realPath.substring(0,realPath.lastIndexOf("/"));
      /*------------------------------------------------------------
       ClassLoader的getResource方法使用了utf-8对路径信息进行了编码，当路径
        中存在中文和空格时，他会对这些字符进行转换，这样，得到的往往不是我们想要
        的真实路径，在此，调用了URLDecoder的decode方法进行解码，以便得到原始的
        中文及空格路径
      -------------------------------------------------------------*/
      try{
        realPath=java.net.URLDecoder.decode(realPath,"utf-8");
       }catch(Exception e){throw new RuntimeException(e);}
       return realPath;
    }//getAppPath定义结束
    @SuppressWarnings("unchecked")
	public static Object jstoob(String json,Class<?> beanClass) {
        Gson gson = new Gson();
        Object res = gson.fromJson(json, beanClass);
        return res;
    }
}
