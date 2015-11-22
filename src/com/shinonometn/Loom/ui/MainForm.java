package com.shinonometn.Loom.ui;

import com.shinonometn.Loom.Program;
import com.shinonometn.Loom.common.ConfigModule;
import com.shinonometn.Loom.common.Logger;
import com.shinonometn.Loom.common.Networks;
import com.shinonometn.Loom.common.Toolbox;
import com.shinonometn.Loom.core.Messenger.ShuttleEvent;
import com.shinonometn.Loom.core.Shuttle;
import com.shinonometn.Loom.resource.Resource;

import javax.swing.*;
import javax.swing.Timer;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.*;
import java.net.NetworkInterface;
import java.text.SimpleDateFormat;
import java.util.*;


/**
 * Created by catten on 15/10/20.
 */
public class MainForm extends JFrame implements ActionListener,ShuttleEvent,WindowListener, MouseListener{

    JTextField t_username;
    JPasswordField t_password;

    JLabel stat_icon;

    JButton btn_login;

    JCheckBoxMenuItem micb_remember;
    JCheckBoxMenuItem micb_hideOnIconfied;
    JCheckBoxMenuItem micb_notShownAtLaunch;

    JComboBox<String> cb_netcard;

    JList<String> list1;
    JScrollPane scrollPane;
    DefaultListModel<String> listModel;

    JMenuBar menuBar;

    //JMenu menuOptions;
    JMenu menuLogs;
    JCheckBoxMenuItem micb_showInfo;
    JCheckBoxMenuItem micb_Log;
    JCheckBoxMenuItem micb_printLog;

    JRadioButtonMenuItem rbmi_AutoModeBoth;
    JRadioButtonMenuItem rbmi_AutoModeOnline;
    JRadioButtonMenuItem rbmi_AutoModeOffline;

    JMenuItem menuItemSetAutoOnline;
    JMenuItem menuItemStateAutoOnline;
    JMenuItem menuItemCleanInfo;
    JMenuItem menuItemCleanLogs;
    JMenuItem menuItemSaveProfile;

    //JMenu menuAbout;
    JMenuItem menuItemAbout;
    JMenuItem menuItemHelp;

    MenuItem menuItemOnline;
    MenuItem menuItemState;
    MenuItem menuItemExit;

    TrayIcon trayIcon;

    Shuttle shuttle;
    Vector<NetworkInterface> nf;
    Resource resource = Resource.getResource();
    Timer timer;
    private boolean timerAlertedFlag = false;

    //图标资源
    ImageIcon icon_online = new ImageIcon(getClass().getResource("/com/shinonometn/Loom/resource/img/link.png"));
    ImageIcon icon_offline = new ImageIcon(getClass().getResource("/com/shinonometn/Loom/resource/img/link_break.png"));
    ImageIcon icon_linking = new ImageIcon(getClass().getResource("/com/shinonometn/Loom/resource/img/link_go.png"));
    ImageIcon icon_app = new ImageIcon(getClass().getResource("/com/shinonometn/Loom/resource/img/package_link.png"));
    ImageIcon icon_dev = new ImageIcon(getClass().getResource("/com/shinonometn/Loom/resource/img/bomb.png"));

    Image tray_online = Toolkit.getDefaultToolkit().getImage(getClass().getResource("/com/shinonometn/Loom/resource/img/package_link 2.png"));
    Image tray_linking = Toolkit.getDefaultToolkit().getImage(getClass().getResource("/com/shinonometn/Loom/resource/img/package_go.png"));
    Image tray_offline = Toolkit.getDefaultToolkit().getImage(getClass().getResource("/com/shinonometn/Loom/resource/img/package.png"));
    Image image_app = Toolkit.getDefaultToolkit().getImage(getClass().getResource("/com/shinonometn/Loom/resource/img/package_link.png"));
//--------

    public MainForm(){
        super("Loom");
        if(Toolbox.getSystemName().contains("mac")) {
            com.apple.eawt.Application.getApplication().setDockIconImage(image_app);
        }
        setMinimumSize(new Dimension(200, 217));
        setSize(ConfigModule.windowWidth, ConfigModule.windowHeight);
        setIconImage(image_app);
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setupUI();
        setupTray();
        setVisible(!ConfigModule.notShownAtLaunch);
        setupEvent();
    }

    //设置界面
    private void setupUI(){
        menuBar = new JMenuBar();

        //menuOptions = new JMenu("选项");
        //-
        micb_Log = new JCheckBoxMenuItem("启用日志");
        micb_Log.setSelected(ConfigModule.useLog);
        micb_remember = new JCheckBoxMenuItem("自动保存设置");
        micb_remember.setSelected(ConfigModule.autoSaveSetting);
        micb_printLog = new JCheckBoxMenuItem("输出日志到终端");
        micb_printLog.setSelected(ConfigModule.outPrintLog);
        micb_hideOnIconfied = new JCheckBoxMenuItem("最小化时隐藏");
        micb_hideOnIconfied.setSelected(ConfigModule.hideOnIconified);
        micb_showInfo = new JCheckBoxMenuItem("显示连接信息");
        micb_showInfo.setSelected(ConfigModule.showInfo);
        micb_notShownAtLaunch = new JCheckBoxMenuItem("启动时不显示窗体");
        micb_notShownAtLaunch.setSelected(ConfigModule.notShownAtLaunch);

        rbmi_AutoModeBoth = new JRadioButtonMenuItem("上线和下线");
        if(ConfigModule.autoOnlineMode.equals("both")) rbmi_AutoModeBoth.setSelected(true);
        rbmi_AutoModeOnline = new JRadioButtonMenuItem("仅上线");
        if(ConfigModule.autoOnlineMode.equals("online")) rbmi_AutoModeOnline.setSelected(true);
        rbmi_AutoModeOffline = new JRadioButtonMenuItem("仅下线");
        if(ConfigModule.autoOnlineMode.equals("both")) rbmi_AutoModeOffline.setSelected(true);
        ButtonGroup buttonGroup = new ButtonGroup();
        buttonGroup.add(rbmi_AutoModeBoth);
        buttonGroup.add(rbmi_AutoModeOnline);
        buttonGroup.add(rbmi_AutoModeOffline);

        menuItemSetAutoOnline = new JMenuItem((ConfigModule.allowAutoMode()?"关闭自动上下线":"设置自动上下线"));
        Logger.log(String.format(
                        "Auto online/offline mode was %s Mode: %s",
                        ConfigModule.allowAutoMode() ? "opend." : "closed.",
                        ConfigModule.autoOnlineMode)
        );
        menuItemStateAutoOnline = new JMenuItem();
        menuItemStateAutoOnline.setEnabled(false);
        updateAutoModeState();

        menuItemCleanLogs = new JMenuItem("清除日志目录");
        menuItemSaveProfile = new JMenuItem("立即保存设置");
        menuItemCleanInfo = new JMenuItem("清除链接信息");
        menuItemCleanInfo.setEnabled(ConfigModule.showInfo);

        menuItemState = new MenuItem("状态：下线");
        menuItemState.setEnabled(false);
        menuItemOnline = new MenuItem("上线");
        menuItemExit = new MenuItem("退出");

        JMenu menu = new JMenu("设置");
        menu.add(micb_hideOnIconfied);
        menu.add(micb_notShownAtLaunch);
        menu.add(new JPopupMenu.Separator());
        menu.add(menuItemSetAutoOnline);
        menu.add(menuItemStateAutoOnline);
        JMenu submenu1 = new JMenu("自动上下线方式");
            submenu1.add(rbmi_AutoModeBoth);
            submenu1.add(rbmi_AutoModeOnline);
            submenu1.add(rbmi_AutoModeOffline);
        menu.add(submenu1);
        menu.add(new JPopupMenu.Separator());
        menu.add(new JPopupMenu.Separator());
        menu.add(micb_remember);
        menu.add(menuItemSaveProfile);
        menuBar.add(menu);

        menuLogs = new JMenu("日志");
        menuLogs.add(micb_Log);
        menuLogs.add(micb_printLog);
        menuLogs.add(micb_showInfo);
        menuLogs.add(new JPopupMenu.Separator());
        menuLogs.add(menuItemCleanInfo);
        menuLogs.add(menuItemCleanLogs);
        menuBar.add(menuLogs);

        menu = new JMenu("关于");
        menuItemHelp = new JMenuItem("帮助");
        menuItemAbout = new JMenuItem("关于软件");
        menu.add(menuItemHelp);
        menu.add(new JPopupMenu.Separator());
        menu.add(menuItemAbout);
        menu.add(new JPopupMenu.Separator());
        JMenuItem m1;
        m1 = new JMenuItem(Program.appName);
        m1.setEnabled(false);
        menu.add(m1);
        m1 = new JMenuItem("Pupa version:3.6");
        m1.setEnabled(false);
        menu.add(m1);
        m1 = new JMenuItem("Amnoon Auth. v3.6.9");
        m1.setEnabled(false);
        menu.add(m1);
        if(ConfigModule.isFakeMode()){
            m1 = new JMenuItem("Fake Mode on");
            m1.setEnabled(false);
            menu.add(m1);
        }
        menuBar.add(menu);

        if(Toolbox.getSystemName().contains("mac")){
            com.apple.eawt.Application.getApplication().setDefaultMenuBar(menuBar);
        }else setJMenuBar(menuBar);

        setLayout(new GridBagLayout());
        GridBagConstraints gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        Insets left_inset = new Insets(2,10,2,0);
        Insets right_inset = new Insets(2,0,2,10);
        Insets normal_inset = new Insets(2,2,2,2);

        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 4;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = normal_inset;
        if(ConfigModule.isFakeMode()){
            add(new JLabel(
                            Program.appName + " (Fake Mode)",
                            new ImageIcon(getClass().getResource("/com/shinonometn/Loom/resource/img/key.png")),
                            JLabel.CENTER
                    ), gridBagConstraints
            );
        }else
            add(new JLabel(Program.appName,icon_app,JLabel.CENTER), gridBagConstraints);

        gridBagConstraints.gridy++;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.weightx = 0;
        gridBagConstraints.insets = left_inset;
        add(new JLabel("用户名:",JLabel.RIGHT),gridBagConstraints);

        gridBagConstraints.gridx++;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.weightx = 1;
        gridBagConstraints.insets = right_inset;
        t_username = new JTextField();
        t_username.setText(ConfigModule.username);
        add(t_username, gridBagConstraints);

        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy++;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.weightx = 0;
        gridBagConstraints.insets = left_inset;
        add(new JLabel("密码:",JLabel.RIGHT),gridBagConstraints);

        gridBagConstraints.gridx++;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.insets = right_inset;
        t_password = new JPasswordField();
        t_password.setText(ConfigModule.password);
        add(t_password, gridBagConstraints);

        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy++;
        gridBagConstraints.insets = left_inset;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.weightx = 0;
        add(new JLabel("网卡:",JLabel.RIGHT),gridBagConstraints);

        gridBagConstraints.gridx++;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.insets = right_inset;
        cb_netcard = new JComboBox<>();
        nf = Networks.getNetworkInterfaces(false);//获取网卡列表
        if(nf != null && nf.size() > 0){
            for(NetworkInterface n:nf){
                cb_netcard.addItem(n.getDisplayName());
                if(ConfigModule.defaultInterface.equals(n.getDisplayName())) cb_netcard.setSelectedIndex(cb_netcard.getItemCount() - 1);
            }
        }else{
            cb_netcard.addItem("找不到可用网卡");
        }
        add(cb_netcard, gridBagConstraints);

        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy++;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.insets = left_inset;
        gridBagConstraints.weightx = 0.5;
        stat_icon = new JLabel(icon_offline,JLabel.CENTER);
        add(stat_icon,gridBagConstraints);

        gridBagConstraints.gridx += 2;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.insets = right_inset;
        btn_login = new JButton("上线");
        add(btn_login, gridBagConstraints);

        gridBagConstraints.gridy++;
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridwidth = 4;
        gridBagConstraints.insets = normal_inset;
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1;
        gridBagConstraints.weighty = 1;
        listModel = new DefaultListModel<String>();
        list1 = new JList<>(listModel);
        scrollPane = new JScrollPane(list1);
        scrollPane.setBorder(new TitledBorder("信息"));
        add(scrollPane, gridBagConstraints);
        scrollPane.setVisible(ConfigModule.showInfo);

        //检查一下有没有可用网卡，没有则不给操作
        if(nf == null ||nf.size() <= 0){
            setVisible(true);
            lockInputUI();
            JOptionPane.showMessageDialog(
                    this,
                    "没有找到可用的网卡。\n如果是有线网卡，请接入网线并确保已经连接到网络\n如果是无线网卡，请检查系统是否能正常识别你的网卡",
                    getTitle(),
                    JOptionPane.WARNING_MESSAGE
            );
        }
    }

    //事件监听设置
    private void setupEvent(){
        addWindowListener(this);

        ActionListener actionListener;

        btn_login.addActionListener(this);
        menuItemOnline.addActionListener(this);
        menuItemExit.addActionListener(this);


        actionListener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if(e.getSource() == micb_notShownAtLaunch){
                    ConfigModule.notShownAtLaunch = micb_notShownAtLaunch.isSelected();
                }else if(e.getSource() == menuItemSaveProfile){ //立即保存配置

                    applyProfile();
                    ConfigModule.writeProfile();
                    trayPopMessage(getTitle(),"日志已保存");
                }else if(e.getSource() == micb_printLog){

                    ConfigModule.outPrintLog = micb_printLog.isSelected();
                }else if(e.getSource() == micb_hideOnIconfied){

                    ConfigModule.hideOnIconified = micb_hideOnIconfied.isSelected();
                }else if(e.getSource() == menuItemCleanLogs){
                    int count;
                    if((count = Logger.clearLog()) >= 0){
                        JOptionPane.showMessageDialog(null, count + " 个日志已清理.", getTitle(), JOptionPane.INFORMATION_MESSAGE);
                    }else{
                        JOptionPane.showMessageDialog(null, "清理日志失败", getTitle(), JOptionPane.WARNING_MESSAGE);
                    }
                }else if(e.getSource() == micb_remember){ //保存用户设置

                    ConfigModule.autoSaveSetting = micb_remember.isSelected();
                    if(ConfigModule.autoSaveSetting) ConfigModule.writeProfile();
                }else if(e.getSource() == micb_Log){ //设置是否启动log

                    ConfigModule.useLog = !ConfigModule.useLog;
                    micb_Log.setSelected(ConfigModule.useLog);
                    if(Program.isDeveloperMode() && ConfigModule.useLog){
                        trayPopMessage("Developer Mode On","您开启了开发者模式，您的账号信息有可能会被记录到日志文件内。");
                    }
                }else if(e.getSource() == menuItemCleanInfo){

                    listModel.clear();
                }else if(e.getSource() == menuItemAbout){
                    JOptionPane.showMessageDialog(
                            null,
                            resource.getResourceText("/com/shinonometn/Loom/resource/about.txt"),
                            "关于 Loom",
                            JOptionPane.INFORMATION_MESSAGE,
                            icon_app
                    );
                }else if(e.getSource() == menuItemHelp){
                    String helpInfo = (ConfigModule.isFakeMode() ?
                            "Hey guy.\nYou maybe already read the help doc. under console mode.\nThe help info only for green hands. :P"
                            :resource.getResourceText("/com/shinonometn/Loom/resource/helpInfo.txt"));
                    JOptionPane.showMessageDialog(null, helpInfo, "帮助", JOptionPane.INFORMATION_MESSAGE, icon_app);
                }else if(e.getSource() == micb_showInfo){

                    ConfigModule.showInfo = micb_showInfo.isSelected();
                    menuItemCleanInfo.setEnabled(ConfigModule.showInfo);
                    scrollPane.setVisible(ConfigModule.showInfo);
                    if(ConfigModule.showInfo && getHeight() < 240){
                        setSize(getWidth(), 240);
                    }else if(!ConfigModule.showInfo){
                        setSize(getWidth(),getMinimumSize().height);
                    }
                }

                if(ConfigModule.autoSaveSetting) applyProfile();
            }
        };
        micb_showInfo.addActionListener(actionListener);
        menuItemCleanInfo.addActionListener(actionListener);
        micb_Log.addActionListener(actionListener);
        micb_remember.addActionListener(actionListener);
        micb_notShownAtLaunch.addActionListener(actionListener);
        micb_printLog.addActionListener(actionListener);
        menuItemCleanLogs.addActionListener(actionListener);
        menuItemSaveProfile.addActionListener(actionListener);
        micb_hideOnIconfied.addActionListener(actionListener);
        menuItemAbout.addActionListener(actionListener);
        menuItemHelp.addActionListener(actionListener);

        actionListener = new ActionListener() {

            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("HH:mm");
            String date = simpleDateFormat.format(new Date());

            @Override
            public void actionPerformed(ActionEvent e) {
                if(e.getSource() == rbmi_AutoModeBoth){
                    ConfigModule.autoOnlineMode = "both";
                    updateAutoModeState();
                }else if(e.getSource() == rbmi_AutoModeOnline){
                    ConfigModule.autoOnlineMode = "online";
                    updateAutoModeState();
                }else if(e.getSource() == rbmi_AutoModeOffline){
                    ConfigModule.autoOnlineMode = "offline";
                    updateAutoModeState();
                }else if(e.getSource() == menuItemSetAutoOnline){
                    if(ConfigModule.allowAutoMode()){
                        int sele = JOptionPane.showConfirmDialog(getOwner(),"关闭自动上下线功能？",getTitle(),JOptionPane.YES_NO_OPTION,JOptionPane.QUESTION_MESSAGE);
                        if(sele == JOptionPane.YES_OPTION){
                            menuItemSetAutoOnline.setText("设置定时上下线");
                            ConfigModule.autoOfflineTime = "";
                            ConfigModule.autoOnlineTime = "";
                            //menuItemStateAutoOnline.setText("定时上下线已关闭");
                            Logger.log("Auto-online mode off.");
                        }
                    }else{
                        String filed = JOptionPane.showInputDialog(getOwner(),"请输入一个时间范围（例如01:00-03:00）");
                        if(!filed.equals("")){
                            String[] fieldSplit = filed.split("\\-");
                            if(fieldSplit.length == 2){
                                ConfigModule.autoOnlineTime = fieldSplit[0];
                                ConfigModule.autoOfflineTime = fieldSplit[1];
                            }
                            if(ConfigModule.allowAutoMode()) {
                                JOptionPane.showMessageDialog(getOwner(), "已设置！", getTitle(), JOptionPane.INFORMATION_MESSAGE);
                                menuItemSetAutoOnline.setText("关闭定时上下线");
                                timer.setDelay(10000);
                                Logger.log("Auto-Online mode on.");
                            }else{
                                JOptionPane.showMessageDialog(getOwner(), "启动自动上下线功能失败，请检查输入",getTitle(),JOptionPane.WARNING_MESSAGE);
                            }
                        }
                    }
                    updateAutoModeState();
                }else if(e.getSource() == timer){
                    if(ConfigModule.allowAutoMode()){
                        String timeNow = simpleDateFormat.format(new Date());
                        if(Program.isDeveloperMode()) Logger.log("Timer tick. Check Time: " + timeNow + "");
                        if(shuttle == null){
                            if(ConfigModule.autoOnlineMode.equals("both") || ConfigModule.autoOnlineMode.equals("online")){
                                if(timeNow.equals(ConfigModule.autoOnlineTime)){
                                    if(!timerAlertedFlag){
                                        onClick_btn_login();
                                        timerAlertedFlag = true;
                                        Logger.log("Timer auto click login button because reach the online time point.");
                                    }
                                }else timerAlertedFlag = false;
                            }
                        }else{
                            if(ConfigModule.autoOnlineMode.equals("both") || ConfigModule.autoOnlineMode.equals("offline")){
                                if(timeNow.equals(ConfigModule.autoOfflineTime)){
                                    if(!timerAlertedFlag){
                                        onClick_btn_login();
                                        timerAlertedFlag = true;
                                        Logger.log("Timer auto click login button because reach the offline time point.");
                                    }
                                }else timerAlertedFlag = false;
                            }
                        }
                    }else{
                        if(Program.isDeveloperMode()) Logger.log("AutoMode is false.");
                        //timer.setDelay(30000);
                    }
                }
            }
        };
        menuItemSetAutoOnline.addActionListener(actionListener);
        rbmi_AutoModeBoth.addActionListener(actionListener);
        rbmi_AutoModeOnline.addActionListener(actionListener);
        rbmi_AutoModeOffline.addActionListener(actionListener);
        timer = new Timer(10000,actionListener);
        timer.start();

    }

    //-----系统托盘图标------------------------------------------
    private void setupTray(){
        Logger.log("Try to setup tray.");
        if(trayIcon == null){
            if(SystemTray.isSupported()){
                SystemTray systemTray = SystemTray.getSystemTray();
                trayIcon = new TrayIcon(tray_offline);
                trayIcon.setToolTip("Loom");
                trayIcon.addMouseListener(this);
                PopupMenu popupMenu = new PopupMenu();
                popupMenu.add(menuItemState);
                popupMenu.add(menuItemOnline);
                popupMenu.addSeparator();
                popupMenu.add(menuItemExit);
                trayIcon.setPopupMenu(popupMenu);

                try {
                    systemTray.add(trayIcon);
                    Logger.log("Add TrayIcon success.");
                } catch (AWTException e) {
                    Logger.error("Add TrayIcon to System Tray failed.");
                }
            }else{
                Logger.log("Tray not supported.");
                ConfigModule.hideOnIconified = false;
            }
        }
    }

    public void setOfflineIcon(){
        stat_icon.setIcon(icon_offline);
        if(trayIcon != null){
            trayIcon.setImage(tray_offline);
            setTrayTip("状态：下线");
        }
    }

    public void setOnlineIcon(){
        btn_login.setText("下线");
        stat_icon.setText("");
        menuItemOnline.setLabel(btn_login.getText());
        stat_icon.setIcon(icon_online);
        if(trayIcon != null){
            trayIcon.setImage(tray_online);
            setTrayTip("状态：在线");
        }
    }

    public void setLinkingIcon(){
        stat_icon.setIcon(icon_linking);
        if(trayIcon != null){
            trayIcon.setImage(tray_linking);
            setTrayTip("状态：重新链接中");
        }
    }

    public void setTrayTip(String tips){
        if(trayIcon != null){
            trayIcon.setToolTip(tips);
            menuItemState.setLabel(tips);
        }
    }

    public void trayPopMessage(String title,String content){
        if(trayIcon != null){
            if(!Toolbox.getSystemName().contains("mac")){
                trayIcon.displayMessage(title, content, TrayIcon.MessageType.INFO);
            }else{
                com.apple.eawt.Application.getApplication().requestUserAttention(true);
            }
        }
    }

//--------------------------------------------------------

    public void updateAutoModeState(){
        if(ConfigModule.allowAutoMode()){
            Logger.log(String.format("Online : %s; Offline: %s",ConfigModule.autoOnlineTime,ConfigModule.autoOfflineTime));
            switch (ConfigModule.autoOnlineMode) {
                case "both":
                    menuItemStateAutoOnline.setText(
                            String.format("将于%s上线，%s下线", ConfigModule.autoOnlineTime, ConfigModule.autoOfflineTime)
                    );
                    Logger.log("Auto-online mode switch to both.");
                    break;
                case "online":
                    menuItemStateAutoOnline.setText(
                            String.format("将于%s上线", ConfigModule.autoOnlineTime)
                    );
                    Logger.log("Auto-online mode switch to online only.");
                    break;
                case "offline":
                    menuItemStateAutoOnline.setText(
                            String.format("将于%s下线", ConfigModule.autoOfflineTime)
                    );
                    Logger.log("Auto-online mode switch to offline only.");
                    break;
            }
        }else menuItemStateAutoOnline.setText("定时上下线已关闭");
    }

    //锁定输入UI
    public void lockInputUI(){
        try{
            t_username.setEnabled(false);
            t_password.setEnabled(false);
            btn_login.setEnabled(false);
            menuItemOnline.setEnabled(false);
            cb_netcard.setEnabled(false);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    //解锁输入UI
    public void unlockInputUI(){
        try{
            t_username.setEnabled(true);
            t_password.setEnabled(true);
            btn_login.setEnabled(true);
            menuItemOnline.setEnabled(true);
            cb_netcard.setEnabled(true);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    //下线的时候修改UI用
    private void uiOffline() {
        btn_login.setText("上线");
        menuItemOnline.setLabel(btn_login.getText());
        unlockInputUI();
        btn_login.setEnabled(true);
        menuItemOnline.setEnabled(true);
        setOfflineIcon();
        stat_icon.setText("");
    }

    private void shuttleOffline(){
        if(shuttle != null){
            shuttle.dispose();
            shuttle = null;
        }
    }

    private void onClick_btn_login(){
        if (shuttle != null && shuttle.isOnline()) {
            lockInputUI();
            btn_login.setText("下线中");
            shuttle.Offline();
        }else{
            lockInputUI();
            btn_login.setText("上线中...");
            shuttle = new Shuttle(nf.get(cb_netcard.getSelectedIndex()),this);
            shuttle.developerMode = Program.isDeveloperMode();
            shuttle.setUsername(t_username.getText());
            shuttle.setPassword(new String(t_password.getPassword()));
            shuttle.start();
            ConfigModule.username = t_username.getText();
            ConfigModule.password = new String(t_password.getPassword());
        }
        menuItemOnline.setLabel(btn_login.getText());
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        //登录按钮的动作
        if(e.getSource() == btn_login || e.getSource() == menuItemOnline){
            onClick_btn_login();
            timerAlertedFlag = true;
        }else if(e.getSource() == menuItemExit){
            if(shuttle != null){
                int result = JOptionPane.showConfirmDialog(null,"是否先下线再退出？",getTitle(),JOptionPane.YES_NO_CANCEL_OPTION);
                switch (result){
                    case JOptionPane.YES_OPTION:
                        onClick_btn_login();
                        System.exit(0);
                    case JOptionPane.NO_OPTION:
                        dispose();
                        Logger.log("Exit without offline.");
                        System.exit(0);
                        break;
                    case JOptionPane.CANCEL_OPTION:
                        //do nothing;
                        break;
                }
            }else{
                dispose();
                System.exit(0);
            }
        }

        if(ConfigModule.autoSaveSetting) ConfigModule.writeProfile();
    }

    public void applyProfile(){
        ConfigModule.username = t_username.getText();
        ConfigModule.password = new String(t_password.getPassword());
        ConfigModule.windowWidth = getWidth();
        ConfigModule.windowHeight = getHeight();
        ConfigModule.outPrintLog = micb_printLog.isSelected();
        ConfigModule.defaultInterface = cb_netcard.getItemAt(cb_netcard.getSelectedIndex());
        ConfigModule.autoSaveSetting = micb_remember.isSelected();
        ConfigModule.useLog = micb_Log.isSelected();
    }

    public void logAtList(String s){
        if(list1 != null && listModel != null && ConfigModule.showInfo){
            listModel.add(listModel.getSize(),s);
        }
    }

    @Override
    public void onMessage(int messageType, String message) {
        switch (messageType){
            //服务器无响应
            case SHUTTLE_SERVER_NO_RESPONSE:{
                if("knock_server_no_response".equals(message)){
                    uiOffline();
                    logAtList("获取认证服务器失败:服务器无响应");
                    JOptionPane.showMessageDialog(
                            this,
                            "敲门无响应\n请检查您所选择的网卡是否已就绪并稍后重试",
                            this.getTitle(),
                            JOptionPane.WARNING_MESSAGE
                    );
                    shuttleOffline();
                }else if("certificate_timeout".equals(message)){
                    uiOffline();
                    logAtList("认证超时，服务器无响应");
                    JOptionPane.showMessageDialog(
                            this,
                            "认证超时，服务器无响应",
                            this.getTitle(),
                            JOptionPane.WARNING_MESSAGE
                    );
                    shuttleOffline();
                }
            }
            break;

            //认证成功
            case SHUTTLE_CERTIFICATE_SUCCESS: {
                logAtList("认证成功");
                logAtList("已上线");
                setOnlineIcon();
                lockInputUI();
                btn_login.setEnabled(true);
                menuItemOnline.setEnabled(true);
            }break;

            //认证失败
            case SHUTTLE_CERTIFICATE_FAILED: {
                logAtList("认证失败:" + message);
                uiOffline();
                shuttleOffline();
                JOptionPane.showMessageDialog(this,"认证失败\n"+message,this.getTitle(),JOptionPane.WARNING_MESSAGE);
            }break;

            //获取Socket成功
            case SHUTTLE_GET_SOCKET_SUCCESS:{
                logAtList("准备工作完成");
            }
            break;

            //端口被占用
            case SHUTTLE_PORT_IN_USE:{
                if("get_connect_socket_failed".equals(message)){
                    logAtList("无法建立链接");
                    JOptionPane.showMessageDialog(
                            this,
                            "无法建立链接，可以稍后再试或者重启程序再试试",
                            this.getTitle(),
                            JOptionPane.WARNING_MESSAGE
                    );
                    uiOffline();
                    shuttleOffline();
                    trayPopMessage(getTitle(),"由于链接问题，上线失败");
                }else if("get_message_socket_failed".equals(message)){
                    logAtList("消息监听端口被占用，获取Socket失败");
                    JOptionPane.showMessageDialog(
                            this,
                            "尝试监听服务器消息失败，可能端口正在被占用。\n不监听服务器消息就无法得知什么时候断网，不过不影响上线。",
                            getTitle(),
                            JOptionPane.INFORMATION_MESSAGE
                    );
                    trayPopMessage(getTitle(), "消息监听出了问题，无法监听服务器消息了。");
                }
            }
            break;

            //服务器回应
            case SHUTTLE_SERVER_RESPONSE:{
                logAtList("服务器IP是 "+message);
            }
            break;

            //找不到服务器
            case SHUTTLE_SERVER_NOT_FOUNT:{
                logAtList("找不到服务器");
                shuttleOffline();
                uiOffline();
            }break;

            //其他错误
            case SHUTTLE_OTHER_EXCEPTION:{
                if("unknown_exception_knocking".equals(message)){
                    uiOffline();
                    shuttleOffline();
                    trayPopMessage(getTitle(),"敲门不成功，上线失败");
                }else if("certificate_info_not_filled".equals(message)){
                    uiOffline();
                    shuttleOffline();
                    JOptionPane.showMessageDialog(this,"密码或帐号为空",getTitle(),JOptionPane.ERROR_MESSAGE);
                }
                logAtList("未知错误:" + message);
            }break;

            //下线
            case SHUTTLE_OFFLINE:{
                logAtList("下线了");
                uiOffline();
            }break;

            //续命成功（雾
            case SHUTTLE_BREATHE_SUCCESS:{
                try {
                    logAtList("[" + (new Date().toString()).split(" ")[3] + "]在线状态续期成功");
                }catch (Exception e){
                    logAtList("在线状态续期成功");
                }
                setOnlineIcon();
                stat_icon.setText("");
            }break;

            //续命失败（大雾
            case SHUTTLE_BREATHE_FAILED:{
                logAtList("服务器否认在线状态");
                setOfflineIcon();
                stat_icon.setText("被下线");
            }break;

            //续命错误（超级雾
            case SHUTTLE_BREATHE_EXCEPTION:{
                stat_icon.setText("");
                if("breathe_timeout".equals(message)){
                    logAtList("续期超时，重试");
                    setLinkingIcon();
                    if(trayIcon != null){
                        trayIcon.setImage(tray_linking);
                    }
                }else if("breathe_time_clear".equals(message)){
                    logAtList("服务器要求在线时常复位");
                }
                else logAtList("呼吸进程遇到错误：" + message);
            }break;

            //消息线程接收到消息
            case SHUTTLE_SERVER_MESSAGE:{
                if("offline".equals(message)){
                    logAtList("服务器要求下线");
                    trayPopMessage(getTitle(), "服务器要求下线");
                    //stat_icon.setText("被下线");
                }else{
                    logAtList("接收到了一条服务器消息:"+message);
                    trayPopMessage("接收到了一条服务器消息",message);
                }
            }break;
        }
    }

    @Override
    public void windowOpened(WindowEvent e) {
        Logger.log("Window Opened");
        if(Program.isDeveloperMode()){
            JOptionPane.showMessageDialog(null, "你开启了开发者模式\n" +
                    "请注意，开发者模式将记录你所有的用户使用信息（包括账号密码）\n" +
                    "请慎用开发者模式，如有需要请清理日志文件以保护隐私。", "Developer Mode on", JOptionPane.WARNING_MESSAGE, icon_dev);
        }
    }

    @Override
    public void windowClosing(WindowEvent e) {
        Logger.log("Window Closing");
        if(ConfigModule.autoSaveSetting){
            ConfigModule.windowWidth = getWidth();
            ConfigModule.windowHeight = getHeight();
            ConfigModule.writeProfile();
            if(Logger.isWriteToFile()){
                Logger.closeLog();
            }
        }
    }

    @Override
    public void windowClosed(WindowEvent e) {
        Logger.log("Window Closed");
    }

    @Override
    public void windowIconified(WindowEvent e) {
        Logger.log("Window Iconified");
        if(ConfigModule.hideOnIconified && !Toolbox.getSystemName().contains("mac")) setVisible(false);
    }

    @Override
    public void windowDeiconified(WindowEvent e) {
        Logger.log("Window Deiconified");
        setVisible(true);
    }

    @Override
    public void windowActivated(WindowEvent e) {
        Logger.log("Window Activated");
        if(Toolbox.getSystemName().contains("mac")) com.apple.eawt.Application.getApplication().requestUserAttention(false);
    }

    @Override
    public void windowDeactivated(WindowEvent e) {
        Logger.log("Window Deactivated");
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        if(e.getSource() == trayIcon){
            if(Toolbox.getSystemName().contains("mac")){
                if(e.getButton() == MouseEvent.BUTTON2 || e.getButton() == MouseEvent.BUTTON3) {
                    setVisible(true);
                }
            }
            else if(e.getButton() == MouseEvent.BUTTON1) {
                setVisible(true);
            }
        }
    }

    @Override
    public void mousePressed(MouseEvent e) {

    }

    @Override
    public void mouseReleased(MouseEvent e) {

    }

    @Override
    public void mouseEntered(MouseEvent e) {

    }

    @Override
    public void mouseExited(MouseEvent e) {

    }

}
