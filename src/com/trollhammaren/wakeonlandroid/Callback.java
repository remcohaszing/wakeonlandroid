package com.trollhammaren.wakeonlandroid;

import java.io.File;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;
import android.widget.RemoteViews;

public class Callback extends Service {
    
    // properties
    /**
     * The port to use for wake on lan. The default is 7
     */
    public static final int DEFAULT_PORT = 7;
    /**
     * The ip address to send the magic packet to. The default value is the
     * broadcast ip address (255.255.255.255).
     */
     public static final String BROADCAST_IP_ADDRESS = "255.255.255.255";
    
    // methods
    @Override
    public void onStart(Intent intent, int startId) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(
                this.getApplicationContext());
        
        int[] allWidgetIds = intent.getIntArrayExtra(
                AppWidgetManager.EXTRA_APPWIDGET_IDS);
        
        try {
            ArrayList<DatagramPacket> packets = this.readConfig(
                    "Android/data/wakeonlan.xml");
            DatagramSocket socket = new DatagramSocket();
            for(DatagramPacket packet : packets) {
                socket.send(packet);
            }
            socket.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
//        for (int widgetId : allWidgetIds) {
//            RemoteViews remoteViews = new RemoteViews(
//                    this.getApplicationContext().getPackageName(),
//                    R.layout.widget_layout);
//            
//            // Register an onClickListener
//            Intent i = new Intent(
//                    this.getApplicationContext(), WidgetProvider.class);
//            
//            i.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
//            i.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, allWidgetIds);
//            
//            PendingIntent pendingIntent = PendingIntent.getBroadcast(
//                    getApplicationContext(), 0, i,
//                    PendingIntent.FLAG_UPDATE_CURRENT);
//            remoteViews.setOnClickPendingIntent(R.id.update, pendingIntent);
//            appWidgetManager.updateAppWidget(widgetId, remoteViews);
//        }
        stopSelf();
    }
    
    /**
     * Unused. The Service interface forces this method to be implemented.
     * 
     * @return null
     */
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    
    /**
     * Creates a magic packet which can be used for wake a lan. The mac address
     * is case insensitive and should be in one of the following formats:<br />
     * 0f:e3:45:6d:a0:84<br />
     * 0f-e3-45-6d-a0-84<br />
     * 0fe3456da084
     * 
     * @param macAddress The mac address to parse into a magic packet
     * @return The magic packet as a byte array
     * @throws Exception Thrown when the mac address is invalid
     */
    public byte[] createMagicPacket(String macAddress) throws Exception {
        String hex = "[a-fA-F0-9]";
        String del = "[:-]";
        if(macAddress.matches("(" + hex + "{2}" + del + "){5}(" + hex + "){2}")) {
//            macAddress = macAddress.replaceAll(del, "");
        } else if(!macAddress.matches("^" + hex + "{12}$")) {
            throw new Exception("Invalid mac address: " + macAddress);
        }
        byte[] packet = new byte[102];
        for(int i = 0; i < 6; i++) {
            packet[i] = (byte) 0xff;
        }
        int i = 6;
        for(String c : macAddress.split(":")) {
            for(int j = 0; j < 16; j++) {
                packet[i + 6 * j] = (byte) Integer.parseInt(c, 16);
            }
            i++;
        }
        return packet;
    }
    
    /**
     * Reads a config file. This method reads the xml config file in the
     * specified relative path on the sd card. The config file should be in the
     * following format:<br />
     * &lt;?xml version="1.0" encoding="UTF-8"?&gt;<br />
     * &lt;config&gt;<br />
     * &nbsp;&nbsp;&lt;wakeonlan port="5" ipaddress="example.com"&gt;<br />
     * &nbsp;&nbsp;&nbsp;&nbsp;&lt;macaddress value="00:00:00:00:00:00"/&gt;
     * <br />
     * &nbsp;&nbsp;&nbsp;&nbsp;&lt;macaddress value="00:00:00:00:00:00"/&gt;
     * <br />
     * &nbsp;&nbsp;&lt;/wakeonlan&gt;<br />
     * &nbsp;&nbsp;&lt;wakeonlan&gt;<br />
     * &nbsp;&nbsp;&nbsp;&nbsp;&lt;macaddress value="00:00:00:00:00:00"/&gt;
     * <br />
     * &nbsp;&nbsp;&nbsp;&nbsp;&lt;macaddress value="00:00:00:00:00:00"/&gt;
     * <br />
     * &nbsp;&nbsp;&lt;/wakeonlan&gt;<br />
     * &lt;/config&gt;<br />
     * The port and ip address parameters are optional. If omitted the default
     * values will be uses. The default port is 7 and the default ip address is
     * the broadcast ip address (255.255.255.255). 
     * 
     * @param file
     * @return
     * @throws Exception
     */
    public ArrayList<DatagramPacket> readConfig(String file) throws Exception {
        File sdcard = Environment.getExternalStorageDirectory();
        File configFile = new File(sdcard, file);
        Document doc = DocumentBuilderFactory.newInstance().
                newDocumentBuilder().parse(configFile);
        doc.getDocumentElement().normalize();
        
        ArrayList<DatagramPacket> packets = new ArrayList<DatagramPacket>();
        NodeList list = doc.getDocumentElement().getChildNodes();
        for (int i = 0; i < list.getLength(); i++) {
            Node wolNode = list.item(i);
            int port = Callback.DEFAULT_PORT;
            String ipAddress = Callback.BROADCAST_IP_ADDRESS;
            if(wolNode.hasAttributes()) {
                Element element = (Element) wolNode;
                if(element.hasAttribute("port")) {
                    port = Integer.parseInt(element.getAttribute("port"));
                }
                if(element.hasAttribute("ipaddress")) {
                    ipAddress = element.getAttribute("ipaddress");
                }
            }
            InetAddress address = InetAddress.getByName(ipAddress);
            NodeList macList = wolNode.getChildNodes();
            for(int j = 0; j < macList.getLength(); j++) {
                Node macItem = macList.item(j);
                if(macItem.hasAttributes()) {
                    byte[] magicPacket = this.createMagicPacket(((Element)
                            macList.item(j)).getAttribute("value"));
                    DatagramPacket packet =new DatagramPacket(magicPacket,
                            magicPacket.length, address, port);
                    packets.add(packet);
                }
            }
        }
        return packets;
    }
}
