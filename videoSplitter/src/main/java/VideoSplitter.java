import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * ref：
 *
 * https://github.com/kejiefu/spring-boot-service-parent/blob/c3984e40a3728ead1445950586c2375d8f4150ce/spring-boot-project/
 * spring-boot-video/src/main/java/project/controller/VideoFileOperate.java
 */

public class VideoSplitter {

    //一段视频的size
    private static long blockSize = 1024*1024*5;

    /**
     * 分割视频
     *
     *按照blockSize来分
     */
    private static void cutVideo(String filename)throws IOException{
        File file = new File(filename);
        if(!file.exists()){
            throw new FileNotFoundException(file+"not found");
        }
        if (!filename.endsWith(".mp4")) {
            throw new IOException("只接受mp4格式");
        }

        //时间长度00:00:00格式
        String time = getVideoTime(file);
        //视频秒数
        int seconds = fromTimeToSecond(time);
        long length = getVideoFileLength(file);
        List<String> cutedVideoPaths = new ArrayList<String>();
        if(length<blockSize){
            //直接返回
            cutedVideoPaths.add(filename);
        }
        else{
            int partitionNum = (int)(length/blockSize);
            long remainSize = length % blockSize;
            int cutNum;
            if(remainSize>0){
                cutNum = partitionNum+1;
            }else{
                cutNum = partitionNum;
            }
            int eachSeconds = seconds / cutNum;
            List<String> commands = new ArrayList<String>();
            String fileFolder = file.getParentFile().getAbsolutePath();
            String fileName[] = file.getName().split("\\.");
            commands.add("ffmpeg");
            for (int i = 0; i < cutNum; i++) {
                commands.add("-i");
                commands.add(filename);
                commands.add("-ss");
                commands.add(parseTimeToString(eachSeconds * i));
                if (i != cutNum - 1) {
                    commands.add("-t");
                    commands.add(parseTimeToString(eachSeconds));
                }
                commands.add("-acodec");
                commands.add("copy");
                commands.add("-vcodec");
                commands.add("copy");
                commands.add(fileFolder + File.separator + fileName[0] + "_part" + i + "." + fileName[1]);
                commands.add("-y");
                //cutedVideoPaths.add(fileFolder + File.separator + fileName[0] + "_part" + i + "." + fileName[1]);
            }
            runCommand(commands);
        }
    }

    /**
     * 分割视频,可以自定义划分的数量
     * @param filename
     * @param cutNum　划分数量
     * @throws IOException
     */

    private static void cutVideo(String filename,int cutNum)throws IOException{
        File file = new File(filename);
        if(!file.exists()){
            throw new FileNotFoundException(file+"not found");
        }
        if (!filename.endsWith(".mp4")) {
            throw new IOException("只接受mp4格式");
        }

        //时间长度00:00:00格式
        String time = getVideoTime(file);
        //视频秒数
        int seconds = fromTimeToSecond(time);
        long length = getVideoFileLength(file);

        int eachSeconds = seconds / cutNum;
        List<String> commands = new ArrayList<String>();
        String fileFolder = file.getParentFile().getAbsolutePath();
        String fileName[] = file.getName().split("\\.");
        commands.add("ffmpeg");
        for (int i = 0; i < cutNum; i++) {
            commands.add("-i");
            commands.add(filename);
            commands.add("-ss");
            commands.add(parseTimeToString(eachSeconds * i));
            if (i != cutNum - 1) {
                commands.add("-t");
                commands.add(parseTimeToString(eachSeconds));
            }
            commands.add("-acodec");
            commands.add("copy");
            commands.add("-vcodec");
            commands.add("copy");
            commands.add(fileFolder + File.separator + fileName[0] + "_part" + i + "." + fileName[1]);
            commands.add("-y");
            //cutedVideoPaths.add(fileFolder + File.separator + fileName[0] + "_part" + i + "." + fileName[1]);
        }
        runCommand(commands);

    }



    /**
     *
     * 将秒表示时长转为00:00:00格式
     *
     *
     */
    private static String parseTimeToString(int seconds){

        int second = seconds%60;
        int minute = second/60;
        if(minute<60){
            return minute+":"+second;
        }else if(minute==60){
            return "1:00:"+second;
        }
        else{
            int hour = minute / 60;
            minute = minute%60;
            return hour+":"+minute+":"+second;
        }
    }

    /**
     * JAVA执行cmd命令方法
     *
     * @param command 存储着所要执行的命令
     * @return 执行结果
     *
     */

    private static synchronized CmdResult runCommand(List<String> command) {

        CmdResult cmdResult = new CmdResult(false, "");
        ProcessBuilder builder = new ProcessBuilder(command);
        builder.redirectErrorStream(true);
        try {
            Process process = builder.start();
            final StringBuilder stringBuilder = new StringBuilder();
            final InputStream inputStream = process.getInputStream();
            //启动新线程为异步读取缓冲器，防止线程阻塞
            new Thread(new Runnable() {
                @Override
                public void run() {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                    String line;
                    try {
                        while ((line = reader.readLine()) != null) {
                            stringBuilder.append(line);
                        }
                        reader.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }).start();
            process.waitFor();
            cmdResult.setSuccess(true);
            cmdResult.setMsg(stringBuilder.toString());
        } catch (Exception e) {
            throw new RuntimeException("ffmpeg执行异常" + e.getMessage());
        }
        return cmdResult;
    }

    /**
     * 获取视频文件时间长度
     *
     * @param  file file
     * @return hh::MM::SS
     * @author alex
     * @throws FileNotFoundException not found
     */
    private static String getVideoTime(File file) throws FileNotFoundException{


        if(!file.exists()){
            throw new FileNotFoundException(file+"is not found.");
        }

        //JAVA运行命令行
        List<String>commands = new ArrayList<String>();
        commands.add("ffmpeg");
        commands.add("-i");
        commands.add(file.getAbsolutePath());
        CmdResult cmdResult = runCommand(commands);
        String message = cmdResult.getMsg();
        if(cmdResult.isSuccess()){
            Pattern pattern = Pattern.compile("Duration: \\d{2}:\\d{2}:\\d{2}");
            Matcher matcher = pattern.matcher(message);
            String time = "";
            if(matcher.find()){
                time = matcher.group();
            }
            return time.substring(10);
        }
        else{
            System.out.println("fail");
            return "";
        }
    }

    /**
     * 获取文件大小
     *
     * @param file 去的文件长度，单位为字节b
     * @return 文件长度的字节数
     * @throws FileNotFoundException 文件未找到异常
     */
    private static long getVideoFileLength(File file) throws FileNotFoundException {
        if (!file.exists()) {
            throw new FileNotFoundException(file.getAbsolutePath() + "不存在");
        }
        return file.length();
    }

    /**
     * 把字符串时间转换回int的时间
     * @param time string
     * @return int seconds
     */

    private static int fromTimeToSecond(String time){
        Pattern pattern = Pattern.compile("\\d{2}:\\d{2}:\\d{2}");
        Matcher matcher = pattern.matcher(time);
        if(!matcher.matches()){
            try{
                throw new Exception("时间格式不正确");
            }catch(Exception e){
                e.printStackTrace();
            }
        }
        String [] times = time.split(":");
        return Integer.parseInt(times[0])*3600+Integer.parseInt(times[1])*60+Integer.parseInt(times[2]);
    }


    public static void main(String [] args)throws IOException{

        String filename = "/home/alex/video-stream-analytics/sample-video/mulan.mp4";
        File file = new File(filename);

        if(file.exists()){
            System.out.println("success");
            cutVideo(filename,4);
        }else{
            System.out.println("not found");
        }

    }


}
