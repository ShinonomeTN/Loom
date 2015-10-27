package com.shinonometn.Loom.ui;

import com.shinonometn.Loom.common.Networks;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.util.*;

/**
 * Created by catten on 15/10/20.
 */
public class MainForm extends JFrame implements ActionListener,ItemListener{

    JTextField t_username;
    JPasswordField t_password;

    JButton btn_login;
    JCheckBox cb_remember;

    JComboBox<String> cb_netcard;

    //JList<JCheckBox> list1;
    JList<String> list1;

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

    public MainForm(){
        super("Loom");
        setSize(240, 400);
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);
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
        Vector<NetworkInterface> nf = Networks.getNetworkInterfaces(false);
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
        add(cb_remember,gridBagConstraints);

        gridBagConstraints.gridx=+2;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.insets = right_inset;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        btn_login = new JButton("上线");
        add(btn_login,gridBagConstraints);

        gridBagConstraints.gridy++;
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.insets = normal_inset;
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1;
        gridBagConstraints.weighty = 1;
        list1 = new JList<>();
        JScrollPane scrollPane = new JScrollPane(list1);
        scrollPane.setBorder(new TitledBorder("信息"));
        String[] strings = new String[50];
        for(int i = 0; i < 50; i++) strings[i] = "Test";
        list1.setListData(strings);
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

    }

    private void setupEvent(){

    }

    @Override
    public void actionPerformed(ActionEvent e) {

    }

    @Override
    public void itemStateChanged(ItemEvent e) {
        if(e.getSource() == cb_netcard){
            //do something
        }
    }
}
