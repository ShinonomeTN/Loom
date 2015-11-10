package com.shinonometn.Loom.ui;

import com.shinonometn.Loom.common.Networks;
import com.shinonometn.Loom.connector.Messanger.ShuttleEvent;
import com.shinonometn.Loom.connector.Shuttle;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.net.DatagramPacket;
import java.net.NetworkInterface;
import java.util.*;

/**
 * Created by catten on 15/10/20.
 */
public class MainForm extends JFrame implements ActionListener,ItemListener,ShuttleEvent{

    JTextField t_username;
    JPasswordField t_password;

    JButton btn_login;
    JCheckBox cb_remember;

    JComboBox<String> cb_netcard;

    //JList<JCheckBox> list1;
    JList<String> list1;
    DefaultListModel<String> listModel = new DefaultListModel<String>();

    JMenuBar menuBar;
    JMenu menuOperation;
    JMenuItem menuItemReset;
    JMenuItem menuItemKillCut;
    JMenuItem menuItemQuit;
    JMenu menuAbout;
    JMenuItem menuItemAbout;
    JMenuItem menuItemRefreshNetCard;
    JMenuItem menuItemHelp;

    JLabel lb_info;

    Shuttle shuttle;
    Thread thread_shuttle;
    Vector<NetworkInterface> nf;
    //Vector<String> vector_Info;

    public MainForm(){
        super("Loom");
        setSize(240, 400);
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        //setResizable(false);
        setupUI();
        setVisible(true);
        setupEvent();
    }

    private void setupUI(){
        menuBar = new JMenuBar();
        setJMenuBar(menuBar);

        menuOperation = new JMenu("操作");
        menuItemReset = new JMenuItem("重新匹配认证服务器");
        menuItemKillCut = new JMenuItem("非正常方式下线");
        menuItemRefreshNetCard = new JMenuItem("刷新网卡列表");
        menuItemQuit = new JMenuItem("退出");

        menuOperation.add(menuItemReset);
        menuOperation.add(menuItemKillCut);
        menuOperation.add(new JPopupMenu.Separator());
        menuOperation.add(menuItemRefreshNetCard);
        menuOperation.add(new JPopupMenu.Separator());
        menuOperation.add(menuItemQuit);

        menuAbout = new JMenu("关于");
        menuItemHelp = new JMenuItem("帮助");
        menuItemAbout = new JMenuItem("关于软件");

        menuAbout.add(menuItemHelp);
        menuAbout.add(new JPopupMenu.Separator());
        menuAbout.add(menuItemAbout);

        menuBar.add(menuOperation);
        menuBar.add(menuAbout);

        setLayout(new GridBagLayout());
        GridBagConstraints gridBagConstraints = new GridBagConstraints();
        Insets left_inset = new Insets(2,10,2,0);
        Insets right_inset = new Insets(2,0,2,10);
        Insets normal_inset = new Insets(2,2,2,2);

        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 1;
        //gridBagConstraints.weightx = 0.3;
        gridBagConstraints.insets = left_inset;
        gridBagConstraints.fill = gridBagConstraints.HORIZONTAL;
        add(new JLabel("用户名",JLabel.RIGHT),gridBagConstraints);

        gridBagConstraints.gridx++;
        //gridBagConstraints.weightx = 0.7;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.insets = right_inset;
        t_username = new JTextField();
        add(t_username,gridBagConstraints);

        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy++;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.weightx = 0;
        gridBagConstraints.insets = left_inset;
        add(new JLabel("密码",JLabel.RIGHT),gridBagConstraints);

        gridBagConstraints.gridx++;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.insets = right_inset;
        t_password = new JPasswordField();
        add(t_password,gridBagConstraints);

        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy++;
        gridBagConstraints.insets = left_inset;
        gridBagConstraints.gridwidth = 1;
        add(new JLabel("网卡",JLabel.RIGHT),gridBagConstraints);

        gridBagConstraints.gridx++;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.insets = right_inset;
        cb_netcard = new JComboBox<>();
        nf = Networks.getNetworkInterfaces(false);
        if(nf != null){
            for(NetworkInterface n:nf){
                cb_netcard.addItem(n.getDisplayName());
            }
        }else{
            cb_netcard.addItem("No Network Interface");
        }
        cb_netcard.addItemListener(this);
        add(cb_netcard, gridBagConstraints);

        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy++;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.insets = left_inset;
        cb_remember = new JCheckBox("保存账户信息");
        add(cb_remember, gridBagConstraints);

        gridBagConstraints.gridx=+2;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.insets = right_inset;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        btn_login = new JButton("上线");
        add(btn_login, gridBagConstraints);

        gridBagConstraints.gridy++;
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.insets = normal_inset;
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1;
        gridBagConstraints.weighty = 1;
        list1 = new JList<>(listModel);
        JScrollPane scrollPane = new JScrollPane(list1);
        scrollPane.setBorder(new TitledBorder("信息"));
        add(scrollPane, gridBagConstraints);

        //Pre-set actions
        if(nf == null){
            setVisible(true);
            t_password.setEditable(false);
            t_username.setEditable(false);
            btn_login.setEnabled(false);
            JOptionPane.showMessageDialog(
                    this,
                    "没有找到可用的网卡。\n如果是有线网卡，请接入网线并确保已经连接到网络\n如果是无线网卡，请检查系统是否能正常识别你的网卡",
                    getTitle(),
                    JOptionPane.WARNING_MESSAGE
            );
        }
    }

    private void lockInputUI(){
        try{
            t_username.setEnabled(false);
            t_password.setEnabled(false);
            btn_login.setEnabled(false);
            cb_netcard.setEnabled(false);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    private void unlockInputUI(){
        try{
            t_username.setEnabled(true);
            t_password.setEnabled(true);
            btn_login.setEnabled(true);
            cb_netcard.setEnabled(true);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    private void setupEvent(){
        cb_remember.addActionListener(this);
        btn_login.addActionListener(this);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if(e.getSource() == btn_login){
            if (shuttle != null) {
                shuttle.Offline();
                lockInputUI();
                btn_login.setText("下线中");
            }else{
                shuttle = new Shuttle(nf.get(cb_netcard.getSelectedIndex()),this);
                shuttle.start();
                lockInputUI();
                btn_login.setText("上线中...");
            }
        }
    }

    @Override
    public void itemStateChanged(ItemEvent e) {
        if(e.getSource() == cb_netcard){
            //do something
        }
    }

    @Override
    public void onMessage(int messageType, String message) {
        switch (messageType){
            case SHUTTLE_SERVER_NO_RESPONSE:{
                if("knock_server_no_response".equals(message)){
                    unlockInputUI();
                    btn_login.setText("上线");
                    listModel.add(listModel.getSize(),"获取认证服务器失败");
                    JOptionPane.showMessageDialog(
                            this,
                            "敲门无响应\n请检查您所选择的网卡是否已连接到校园网",
                            this.getTitle(),
                            JOptionPane.WARNING_MESSAGE
                    );
                    shuttle.dispose();
                    shuttle = null;
                }else if("certificate_timeout".equals(message)){
                    unlockInputUI();
                    btn_login.setText("上线");
                    listModel.add(listModel.getSize(), "认证超时，服务器无响应");
                    JOptionPane.showMessageDialog(
                            this,
                            "认证超时，服务器无响应",
                            this.getTitle(),
                            JOptionPane.WARNING_MESSAGE
                    );
                    shuttle.dispose();
                    shuttle = null;
                }
            }
            break;

            case SHUTTLE_CERTIFICATE_SUCCESS:{
                listModel.add(listModel.getSize(), message);
                unlockInputUI();
                btn_login.setText("下线");
                cb_netcard.setEnabled(false);
            }break;

            case SHUTTLE_GET_SOCKET_SUCCESS:{
                listModel.add(listModel.getSize(),"获取Socket成功");
            }
            break;

            case SHUTTLE_PORT_IN_USE:{
                listModel.add(listModel.getSize(),"目的网卡端口号正在被使用，获取Socket失败");
                JOptionPane.showMessageDialog(
                        this,
                        "端口正在被使用：目的网卡拨号端口被占用\n请查看您是否已经启动了其他拨号器，或者尝试更换目的网卡",
                        this.getTitle(),
                        JOptionPane.WARNING_MESSAGE
                );
                unlockInputUI();
                btn_login.setText("上线");
                shuttle = null;
            }
            break;

            case SHUTTLE_SERVER_RESPONSE:{
                listModel.add(listModel.getSize(),"服务器IP是 "+message);
            }
            break;

            case SHUTTLE_SERVER_NOT_FOUNT:{
                listModel.add(listModel.getSize(),"找不到服务器");
            }break;

            case SHUTTLE_OTHER_EXCEPTION:{
                listModel.add(listModel.getSize(),"未知错误" + message);
            }break;

            case SHUTTLE_OFFLINE:{
                listModel.add(listModel.getSize(),"下线");
                btn_login.setText("上线");
                cb_netcard.setEnabled(true);
                unlockInputUI();
            }break;
        }
    }
}
