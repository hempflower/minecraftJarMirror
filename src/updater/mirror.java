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
			// TODO �Զ����ɵ� catch ��
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
				// TODO �Զ����ɵ� catch ��
				e.printStackTrace();
			}
		     String verjson=readin(getAppPath(mirror.class)+"\\mcdl\\"+li.versions[x].id+"\\"+li.versions[x].id+".json");
		     fomat fo=gson.fromJson(verjson, fomat.class);
		     
		     try {
				downLoadFromUrl(fo.downloads.client.url,li.versions[x].id+".jar",getAppPath(mirror.class)+"\\mcdl\\"+li.versions[x].id);
			} catch (IOException e) {
				// TODO �Զ����ɵ� catch ��
				e.printStackTrace();
			}
		     if(!fo.assetIndex.url.equals("https://launchermeta.mojang.com/mc/assets/legacy/c0fd82e8ce9fbc93119e40d96d5a4e62cfa3f729/legacy.json")){
		    	 try {
		    	 //System.out.println(li.versions[x].id);
		    		 downLoadFromUrl(fo.assetIndex.url,fo.assetIndex.id+".json",getAppPath(mirror.class)+"\\mcdl\\asstes\\indexes\\");
		     	} catch (IOException e) {
		     		// TODO �Զ����ɵ� catch ��
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
		    			 // TODO �Զ����ɵ� catch ��
		    //			 e.printStackTrace();
		    //		 }
		    //	 }else {
			 //   	 try {
			//			downLoadFromUrl("http://resources.download.minecraft.net/"+aobj.objects[y].hash.substring(0, 2)+"/"+aobj.objects[x].hash,aobj.objects[y].hash,getAppPath(mirror.class)+"\\mcdl\\asstes\\objects\\"+aobj.objects[y].hash.substring(0, 2)+aobj.objects[y].hash);
			//		} catch (IOException e) {
			//			// TODO �Զ����ɵ� catch ��
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
			// TODO �Զ����ɵ� catch ��
			e.printStackTrace();
		}
		
		
	}

    public static void  downLoadFromUrl(String urlStr,String fileName,String savePath) throws IOException{  
        URL url = new URL(urlStr);    
        HttpURLConnection conn = (HttpURLConnection)url.openConnection();    
                //���ó�ʱ��Ϊ10��  
        conn.setConnectTimeout(10*1000);  
        //��ֹ���γ���ץȡ������403����  
        conn.setRequestProperty("User-Agent", "Mozilla/4.0 (compatible; MSIE 5.0; Windows NT; DigExt)");  
  
        //�õ�������  
        InputStream inputStream = conn.getInputStream();    
        //��ȡ�Լ�����  
        byte[] getData = readInputStream(inputStream);      
  
        //�ļ�����λ��  
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
  
  
        System.out.println("info:"+url+" ���ɹ��ı��棡");   
  
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
        //����û�����Ĳ����Ƿ�Ϊ��
        if(cls==null) 
         throw new java.lang.IllegalArgumentException("��������Ϊ�գ�");
        ClassLoader loader=cls.getClassLoader();
        //������ȫ������������
        String clsName=cls.getName()+".class";
        //��ô���������ڵİ�
        Package pack=cls.getPackage();
        String path="";
        //���������������������ת��Ϊ·��
        if(pack!=null){
            String packName=pack.getName();
           //�˴����ж��Ƿ���Java������⣬��ֹ�û�����JDK���õ����
           if(packName.startsWith("java.")||packName.startsWith("javax.")) 
              throw new java.lang.IllegalArgumentException("��Ҫ����ϵͳ�࣡");
            //����������У�ȥ�������Ĳ��֣��������ļ���
            clsName=clsName.substring(packName.length()+1);
            //�ж������Ƿ��Ǽ򵥰���������ǣ���ֱ�ӽ�����ת��Ϊ·����
            if(packName.indexOf(".")<0) path=packName+"/";
            else{//�����հ�������ɲ��֣�������ת��Ϊ·��
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
        //����ClassLoader��getResource�������������·����Ϣ�����ļ���
        java.net.URL url =loader.getResource(path+clsName);
        //��URL�����л�ȡ·����Ϣ
        String realPath=url.getPath();
        //ȥ��·����Ϣ�е�Э����"file:"
        int pos=realPath.indexOf("file:");
        if(pos>-1) realPath=realPath.substring(pos+5);
        //ȥ��·����Ϣ���������ļ���Ϣ�Ĳ��֣��õ������ڵ�·��
        pos=realPath.indexOf(path+clsName);
        realPath=realPath.substring(0,pos-1);
        //������ļ��������JAR���ļ���ʱ��ȥ����Ӧ��JAR�ȴ���ļ���
        if(realPath.endsWith("!"))
            realPath=realPath.substring(0,realPath.lastIndexOf("/"));
      /*------------------------------------------------------------
       ClassLoader��getResource����ʹ����utf-8��·����Ϣ�����˱��룬��·��
        �д������ĺͿո�ʱ���������Щ�ַ�����ת�����������õ�����������������Ҫ
        ����ʵ·�����ڴˣ�������URLDecoder��decode�������н��룬�Ա�õ�ԭʼ��
        ���ļ��ո�·��
      -------------------------------------------------------------*/
      try{
        realPath=java.net.URLDecoder.decode(realPath,"utf-8");
       }catch(Exception e){throw new RuntimeException(e);}
       return realPath;
    }//getAppPath�������
    @SuppressWarnings("unchecked")
	public static Object jstoob(String json,Class<?> beanClass) {
        Gson gson = new Gson();
        Object res = gson.fromJson(json, beanClass);
        return res;
    }
}
