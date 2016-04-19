package com.shinonometn.loom.ui;

import com.shinonometn.loom.Program;
import com.shinonometn.loom.common.ConfigModule;
import com.shinonometn.loom.common.Networks;
import com.shinonometn.loom.common.Toolbox;
import com.shinonometn.loom.core.message.ShuttleEvent;
import com.shinonometn.loom.core.Shuttle;
import com.shinonometn.loom.resource.Resource;
import com.shinonometn.Pupa.Pupa;
import org.apache.log4j.Logger;

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

    private static Logger logger = Logger.getLogger("window");

    JTextField t_username;
    JPasswordField t_password;

    JLabel stat_icon;

    JButton btn_login;
    JCheckBox cb_autoOnline;

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
    JMenuItem menuItemHideOnClose;
    JMenuItem menuItemSpecialDays;
    JMenuItem menuItemSpecialOnlineDays;
    JMenuItem menuItemSpecialOfflineDays;

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
    ImageIcon icon_online = new ImageIcon(getClass().getResource("/com/shinonometn/loom/resource/img/link.png"));
    ImageIcon icon_offline = new ImageIcon(getClass().getResource("/com/shinonometn/loom/resource/img/link_break.png"));
    ImageIcon icon_linking = new ImageIcon(getClass().getResource("/com/shinonometn/loom/resource/img/link_go.png"));
    ImageIcon icon_app = new ImageIcon(getClass().getResource("/com/shinonometn/loom/resource/img/package_link.png"));
    ImageIcon icon_dev = new ImageIcon(getClass().getResource("/com/shinonometn/loom/resource/img/bomb.png"));

    Image tray_online;
    Image tray_linking;
    Image tray_offline;
    Image image_app = Toolkit.getDefaultToolkit().getImage(getClass().getResource("/com/shinonometn/loom/resource/img/package_link.png"));
//--------

    public MainForm(){
        super("Loom");

        tray_online = Toolkit.getDefaultToolkit().getImage(
                getClass().getResource("/com/shinonometn/loom/resource/img/" + (Toolbox.isMacOSX() ? "icon_link.png" : "package_link 2.png")));
        tray_linking = Toolkit.getDefaultToolkit().getImage(
                getClass().getResource("/com/shinonometn/loom/resource/img/" + (Toolbox.isMacOSX() ? "icon_loading.png" : "package_go.png")));
        tray_offline = Toolkit.getDefaultToolkit().getImage(
                getClass().getResource("/com/shinonometn/loom/resource/img/" + (Toolbox.isMacOSX() ? "icon_link_alt.png" : "package.png")));

        if(Toolbox.isMacOSX()) {
            com.apple.eawt.Application.getApplication().setDockIconImage(image_app);
        }
        setMinimumSize(new Dimension(200, 240));
        setSize(ConfigModule.windowWidth, ConfigModule.windowHeight);
        setIconImage(image_app);
        setDefaultCloseOperation((ConfigModule.hideOnClose ? WindowConstants.HIDE_ON_CLOSE : WindowConstants.EXIT_ON_CLOSE));
        setLocationRelativeTo(null);
        setupUI();
        setupTray();
        setupEvent();

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

        if(ConfigModule.autoOnline){
            if(!"".equals(ConfigModule.username) && !"".equals(ConfigModule.password)){
                onClick_btn_login();
            }
        }

        setVisible(!ConfigModule.notShownAtLaunch);
        updateAutoModeState();
        JOptionPane.setRootFrame(this);

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
        logger.info(String.format(
                        "Auto online/offline mode was %s Mode: %s",
                        ConfigModule.allowAutoMode() ? "open." : "closed.",
                        ConfigModule.autoOnlineMode)
        );
        menuItemStateAutoOnline = new JMenuItem();
        menuItemStateAutoOnline.setEnabled(false);
        menuItemHideOnClose = new JCheckBoxMenuItem("关闭窗口时隐藏");
        menuItemHideOnClose.setSelected(ConfigModule.hideOnClose);

        menuItemSpecialDays = new JMenuItem("设置例外日");
        menuItemSpecialOnlineDays = new JMenuItem("不上线:");
        menuItemSpecialOnlineDays.setEnabled(false);
        menuItemSpecialOfflineDays = new JMenuItem("不下线:");
        menuItemSpecialOfflineDays.setEnabled(false);

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
        menu.add(menuItemHideOnClose);
        menu.add(new JPopupMenu.Separator());
        menu.add(menuItemSetAutoOnline);
        menu.add(menuItemStateAutoOnline);
        JMenu submenu1 = new JMenu("自动上下线方式");
            submenu1.add(rbmi_AutoModeBoth);
            submenu1.add(rbmi_AutoModeOnline);
            submenu1.add(rbmi_AutoModeOffline);
        menu.add(submenu1);
        menu.add(menuItemSpecialDays);
        menu.add(menuItemSpecialOnlineDays);
        menu.add(menuItemSpecialOfflineDays);
        //menu.add(new JPopupMenu.Separator());
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
        m1 = new JMenuItem("Pupa version:" + Pupa.getVersion());
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

        if(Toolbox.isMacOSX()){
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
        gridBagConstraints.insets = new Insets(10,2,10,2);
        if(ConfigModule.isFakeMode()){
            add(new JLabel(
                            Program.appName + " (Fake Mode)",
                            new ImageIcon(getClass().getResource("/com/shinonometn/loom/resource/img/key.png")),
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
        cb_netcard = new JComboBox<String>();
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
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.insets = left_inset;
        stat_icon = new JLabel(icon_offline,JLabel.CENTER);
        add(stat_icon,gridBagConstraints);

        gridBagConstraints.gridx ++;
        gridBagConstraints.insets = normal_inset;
        gridBagConstraints.weightx = 0.25;
        cb_autoOnline = new JCheckBox("自动上线");
        cb_autoOnline.setSelected(ConfigModule.autoOnline);
        add(cb_autoOnline, gridBagConstraints);

        gridBagConstraints.gridx ++;
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
        listModel = new DefaultListModel<>();
        list1 = new JList<>(listModel);
        scrollPane = new JScrollPane(list1);
        scrollPane.setBorder(new TitledBorder("信息"));
        add(scrollPane, gridBagConstraints);
        scrollPane.setVisible(ConfigModule.showInfo);

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
                    JOptionPane.showMessageDialog(null, "此功能暂时不可用", getTitle(), JOptionPane.WARNING_MESSAGE);
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
                            resource.getResourceText("/com/shinonometn/loom/resource/text/about.txt"),
                            "关于 Loom",
                            JOptionPane.INFORMATION_MESSAGE,
                            icon_app
                    );
                }else if(e.getSource() == menuItemHelp){
                    String helpInfo = (ConfigModule.isFakeMode() ?
                            "Hey guy.\nYou maybe already read the help doc. under console mode.\nThe help info only for green hands. :P"
                            :resource.getResourceText("/com/shinonometn/loom/resource/text/helpInfo.txt"));
                    JOptionPane.showMessageDialog(null, helpInfo, "帮助", JOptionPane.INFORMATION_MESSAGE, icon_app);
                }else if(e.getSource() == micb_showInfo){

                    ConfigModule.showInfo = micb_showInfo.isSelected();
                    menuItemCleanInfo.setEnabled(ConfigModule.showInfo);
                    scrollPane.setVisible(ConfigModule.showInfo);
                    if(ConfigModule.showInfo && getHeight() < 240){
                        setSize(getWidth(), 300);
                        getMinimumSize().setSize(200, 300);
                    }else if(!ConfigModule.showInfo){
                        setSize(getWidth(),getMinimumSize().height);
                        getMinimumSize().setSize(200,240);
                    }
                }else if(e.getSource() == menuItemHideOnClose){

                    ConfigModule.hideOnClose = menuItemHideOnClose.isSelected();
                    setDefaultCloseOperation((ConfigModule.hideOnClose ? WindowConstants.HIDE_ON_CLOSE : WindowConstants.EXIT_ON_CLOSE));
                }else if(e.getSource() == cb_autoOnline){

                    ConfigModule.autoOnline = cb_autoOnline.isSelected();
                }

                if(ConfigModule.autoSaveSetting) applyProfile();
            }
        };
        cb_autoOnline.addActionListener(actionListener);
        micb_showInfo.addActionListener(actionListener);
        micb_Log.addActionListener(actionListener);
        micb_remember.addActionListener(actionListener);
        micb_notShownAtLaunch.addActionListener(actionListener);
        micb_printLog.addActionListener(actionListener);
        micb_hideOnIconfied.addActionListener(actionListener);
        menuItemCleanInfo.addActionListener(actionListener);
        menuItemCleanLogs.addActionListener(actionListener);
        menuItemSaveProfile.addActionListener(actionListener);
        menuItemAbout.addActionListener(actionListener);
        menuItemHelp.addActionListener(actionListener);
        menuItemHideOnClose.addActionListener(actionListener);

        actionListener = new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                if(e.getSource() == menuItemSpecialDays){
                    Object[] possibilities = {"没有特殊日", "周末不下线", "周五和周末不下线","周五上线周日下线","自己编辑表达式..."};
                    String s = (String)JOptionPane.showInputDialog(
                            null,
                            "请选择一个符合心意的选项",
                            "特殊上下线日",
                            JOptionPane.PLAIN_MESSAGE,
                            icon_app,
                            possibilities,
                            "ham"
                    );

                    if ((s != null) && (s.length() > 0)) {
                        switch (s) {
                            case "没有特殊日":
                                ConfigModule.specialDays = "online:Mon,Tue,Wed,Thu,Fri,Sat,Sun;offline:Mon,Tue,Wed,Thu,Fri,Sat,Sun";
                                break;
                            case "周末不下线":
                                ConfigModule.specialDays = "online:Mon,Tue,Wed,Thu,Fri,Sat,Sun;offline:Mon,Tue,Wed,Thu,Fri";
                                break;
                            case "周五和周末不下线":
                                ConfigModule.specialDays = "online:Mon,Tue,Wed,Thu,Fri,Sat,Sun;offline:Mon,Tue,Wed,Thu";
                                break;
                            case "周五上线周日下线":
                                ConfigModule.specialDays = "online:Mon,Tue,Wed,Thu,Fri;offline:Mon,Tue,Wed,Thu,Sun";
                                break;
                            case "自己编辑表达式...":
                                String input = JOptionPane.showInputDialog(getOwner(), "自定义表达式\n\n格式：\nonline:Mon,Tue,Wed,Thu,Fri,Sat,Sun;offline:Mon,Tue,Wed,Thu,Fri,Sat,Sun");
                                if (input != null && input.matches("online:[^:;]*;offline:[^:;]*")) {
                                    ConfigModule.specialDays = input;
                                } else if (input != null) {
                                    JOptionPane.showMessageDialog(getOwner(), "格式不正确");
                                }
                                break;
                        }
                        updateAutoModeState();
                    }
                }else if(e.getSource() == rbmi_AutoModeBoth){
                    ConfigModule.autoOnlineMode = "both";
                    updateAutoModeState();
                }else if(e.getSource() == rbmi_AutoModeOnline){
                    ConfigModule.autoOnlineMode = "online";
                    updateAutoModeState();
                }else if(e.getSource() == rbmi_AutoModeOffline){
                    ConfigModule.autoOnlineMode = "offline";
                    updateAutoModeState();
                }else if(e.getSource() == menuItemSetAutoOnline) {
                    if (ConfigModule.allowAutoMode()) {
                        int sele = JOptionPane.showOptionDialog(
                                getOwner(),
                                "关闭上下线功能？",
                                getTitle(),
                                JOptionPane.YES_NO_CANCEL_OPTION,
                                JOptionPane.QUESTION_MESSAGE,
                                icon_app,
                                new String[]{"是","否","修改时间"},
                                "否"
                        );
                        if (sele == JOptionPane.YES_OPTION) {
                            menuItemSetAutoOnline.setText("设置定时上下线");
                            ConfigModule.autoOfflineTime = "";
                            ConfigModule.autoOnlineTime = "";
                            //menuItemStateAutoOnline.setText("定时上下线已关闭");
                            logger.info("Auto-online mode off.");
                        }else if(sele == JOptionPane.CANCEL_OPTION){
                            setAutoOnlineTime();
                        }
                    } else {
                        setAutoOnlineTime();
                    }
                    updateAutoModeState();
                }
            }
        };
        menuItemSetAutoOnline.addActionListener(actionListener);
        menuItemSpecialDays.addActionListener(actionListener);
        rbmi_AutoModeBoth.addActionListener(actionListener);
        rbmi_AutoModeOnline.addActionListener(actionListener);
        rbmi_AutoModeOffline.addActionListener(actionListener);

        timer = new Timer(10000, new ActionListener() {
            SimpleDateFormat simpleTimeFormat = new SimpleDateFormat("HH:mm");
            SimpleDateFormat simpleWeekFormat = new SimpleDateFormat("EEE",Locale.US);
            String timeNow;
            String weekNow;

            @Override
            public void actionPerformed(ActionEvent e) {
                if(e.getSource() == timer){
                    if(ConfigModule.allowAutoMode()){
                        if(!"".equals(ConfigModule.specialDays)){
                            String[] fields = ConfigModule.specialDays.split(";");
                            if(fields.length == 2){
                                //获取日期
                                Date dateNow = new Date();
                                //格式化日期
                                timeNow = simpleTimeFormat.format(dateNow);
                                weekNow = simpleWeekFormat.format(dateNow);
                                if(Program.isDeveloperMode()) logger.info("Timer tick. Check Time: " + timeNow + "; Check WeekDay: " + weekNow);
                                if(Program.isDeveloperMode()) logger.info(String.format("Raw DateTime: %s %s", timeNow, simpleWeekFormat.format(dateNow)));
                                    if(shuttle == null){//上线动作
                                    if(fields[0].contains(weekNow)){//获得online字段
                                        if(ConfigModule.autoOnlineMode.equals("both") || ConfigModule.autoOnlineMode.equals("online")){
                                            if(timeNow.equals(ConfigModule.autoOnlineTime)){
                                                if(!timerAlertedFlag){
                                                    onClick_btn_login();
                                                    timerAlertedFlag = true;
                                                    if(Program.isDeveloperMode()) logger.info("Timer auto click login button because reach the online time point.");
                                                }
                                            }else timerAlertedFlag = false;
                                        }
                                    }
                                }else{//下线动作
                                    if(fields[1].contains(weekNow)){//获得offline字段
                                        if(ConfigModule.autoOnlineMode.equals("both") || ConfigModule.autoOnlineMode.equals("offline")){
                                            if(timeNow.equals(ConfigModule.autoOfflineTime)){
                                                if(!timerAlertedFlag){
                                                    onClick_btn_login();
                                                    timerAlertedFlag = true;
                                                    if(Program.isDeveloperMode()) logger.info("Timer auto click login button because reach the offline time point.");
                                                }
                                            }else timerAlertedFlag = false;
                                        }
                                    }
                                }
                            }
                        }
                    }else{
                        if(Program.isDeveloperMode()) logger.info("Auto online mode was closed.");
                        //timer.setDelay(30000);
                    }
                }
            }
        });
        timer.start();

    }

    private void setAutoOnlineTime(){
        String filed = JOptionPane.showInputDialog(this, "请输入一个时间范围（例如01:00-03:00）");
        if (!"".equals(filed)) {
            String[] fieldSplit = filed.split("\\-");
            if (fieldSplit.length == 2) {
                ConfigModule.autoOnlineTime = fieldSplit[0];
                ConfigModule.autoOfflineTime = fieldSplit[1];
            }
            if (ConfigModule.allowAutoMode()) {
                JOptionPane.showMessageDialog(getOwner(), "已设置！", getTitle(), JOptionPane.INFORMATION_MESSAGE);
                menuItemSetAutoOnline.setText("关闭定时上下线");
                timer.setDelay(10000);
                logger.info("Auto online mode on.");
            } else {
                JOptionPane.showMessageDialog(getOwner(), "启动自动上下线功能失败，请检查输入", getTitle(), JOptionPane.WARNING_MESSAGE);
            }
        }
    }

    //-----系统托盘图标------------------------------------------
    private void setupTray(){
        logger.info("Try to setup tray.");
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
                    logger.info("Add TrayIcon success.");
                } catch (AWTException e) {
                    logger.error("Add TrayIcon to System Tray failed.");
                }
            }else{
                logger.warn("Tray not supported.");
                ConfigModule.hideOnIconified = false;
            }
        }
    }

    public void setOfflineIcon(){
        stat_icon.setIcon(icon_offline);
        if(trayIcon != null){
            trayIcon.setImage(tray_offline);
            setTrayTip("Loom ：下线");
        }
    }

    public void setOnlineIcon(){
        btn_login.setText("下线");
        stat_icon.setText("");
        menuItemOnline.setLabel(btn_login.getText());
        stat_icon.setIcon(icon_online);
        if(trayIcon != null){
            trayIcon.setImage(tray_online);
            setTrayTip("Loom ：在线");
        }
    }

    public void setLinkingIcon(){
        stat_icon.setIcon(icon_linking);
        if(trayIcon != null){
            trayIcon.setImage(tray_linking);
            setTrayTip("Loom ：链接中");
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
            if(!Toolbox.isMacOSX()){
                trayIcon.displayMessage(title, content, TrayIcon.MessageType.INFO);
            }else{
                //com.apple.eawt.Application.getApplication().requestUserAttention(true);
            }
        }
    }

//--------------------------------------------------------

    public void updateAutoModeState(){
        if(ConfigModule.allowAutoMode()){
            logger.info(String.format("Online : %s; Offline: %s",ConfigModule.autoOnlineTime,ConfigModule.autoOfflineTime));
            switch (ConfigModule.autoOnlineMode) {
                case "both":
                    menuItemStateAutoOnline.setText(
                            String.format("将于%s上线，%s下线", ConfigModule.autoOnlineTime, ConfigModule.autoOfflineTime)
                    );
                    logger.info("Auto-online mode switch to both.");
                    break;
                case "online":
                    menuItemStateAutoOnline.setText(
                            String.format("将于%s上线", ConfigModule.autoOnlineTime)
                    );
                    logger.info("Auto-online mode switch to online only.");
                    break;
                case "offline":
                    menuItemStateAutoOnline.setText(
                            String.format("将于%s下线", ConfigModule.autoOfflineTime)
                    );
                    logger.info("Auto-online mode switch to offline only.");
                    break;
            }
        }else menuItemStateAutoOnline.setText("定时上下线已关闭");

        String[] strings = ConfigModule.getSpecialDays().split(";");
        menuItemSpecialOnlineDays.setText("不上线:" + strings[0]);
        menuItemSpecialOfflineDays.setText("不下线:" + strings[1]);
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
            shuttle.offline();
        }
        shuttle = null;
    }

    private void onClick_btn_login(){
        if (shuttle != null && shuttle.isBreathing()) {
            lockInputUI();
            btn_login.setText("下线中");
            //shuttle.Offline();
            shuttleOffline();
        }else{
            lockInputUI();
            btn_login.setText("上线中...");
            setLinkingIcon();
            shuttle = new Shuttle(nf.get(cb_netcard.getSelectedIndex()),this);
            //shuttle.developerMode = Program.isDeveloperMode();
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
                        if(shuttle != null && shuttle.isBreathing()) shuttleOffline();
                        System.exit(0);
                    case JOptionPane.NO_OPTION:
                        dispose();
                        logger.info("Exit without offline.");
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
            case SERVER_NO_RESPONSE:{
                if("knock_server".equals(message)){
                    uiOffline();
                    logAtList(ConfigModule.isFakeMode() ? "续命失败:找不到长者":"获取认证服务器失败:服务器无响应");
                    JOptionPane.showMessageDialog(
                            this,
                            "敲门无响应\n请检查您所选择的网卡是否已就绪并稍后重试",
                            this.getTitle(),
                            JOptionPane.WARNING_MESSAGE
                    );
                    shuttleOffline();
                }
            }
            break;

            //认证成功
            case CERTIFICATE_SUCCESS: {
                logAtList(ConfigModule.isFakeMode() ? "开始续命了" : "认证成功,已上线");
                if(Program.isDeveloperMode()) logAtList("会话号 " + shuttle.getSessionNo());
                setOnlineIcon();
                lockInputUI();
                btn_login.setEnabled(true);
                menuItemOnline.setEnabled(true);
            }break;

            //认证失败
            case CERTIFICATE_FAILED: {
                if("info_not_filled".equals(message)){
                    uiOffline();
                    shuttleOffline();
                    JOptionPane.showMessageDialog(this,"密码或帐号为空",getTitle(),JOptionPane.ERROR_MESSAGE);
                }else{
                    logAtList((ConfigModule.isFakeMode() ? "续命失败:" : "认证失败:") + message);
                    uiOffline();
                    shuttleOffline();
                    JOptionPane.showMessageDialog(this,"认证失败\n"+message,this.getTitle(),JOptionPane.WARNING_MESSAGE);
                }
            }break;

            case CERTIFICATE_EXCEPTION:{
                if("timeout".equals(message)){
                    uiOffline();
                    logAtList(ConfigModule.isFakeMode() ? "申请续命超时，有人不让你续命":"认证超时，服务器无响应");
                    JOptionPane.showMessageDialog(
                            this,
                            "认证超时，服务器无响应",
                            this.getTitle(),
                            JOptionPane.WARNING_MESSAGE
                    );
                    shuttleOffline();
                }else{
                    logAtList("认证状态不明");
                    JOptionPane.showMessageDialog(this,"认证状态不明\n如果还不能访问网路,请退出程序再试",getTitle(),JOptionPane.WARNING_MESSAGE);
                    unlockInputUI();
                }
            }break;

            //获取Socket成功
            case SOCKET_GET_SUCCESS:{
                logAtList(ConfigModule.isFakeMode() ? "续命准备完成" : "准备工作完成");
            }
            break;

            //端口被占用
            case SOCKET_PORT_IN_USE:{
                if("get_connection_socket_failed".equals(message)){
                    logAtList(ConfigModule.isFakeMode() ? "无法续命" : "无法建立链接");
                    JOptionPane.showMessageDialog(
                            this,
                            "无法建立链接，可以稍后再试或者重启程序再试试。",
                            this.getTitle(),
                            JOptionPane.WARNING_MESSAGE
                    );
                    uiOffline();
                    shuttleOffline();
                    trayPopMessage(getTitle(),"由于链接问题，上线失败");
                }else if("get_message_socket_failed".equals(message)){
                    logAtList(ConfigModule.isFakeMode() ? "有人不许你听长者谈笑风生" : "消息监听端口被占用，获取Socket失败");
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
            case SERVER_RESPONSE_IPADDRESS:{
                logAtList((ConfigModule.isFakeMode() ? "找到长者了 " : "服务器IP是 ")+message);
            }
            break;

            //找不到服务器
            case SERVER_NOT_FOUNT:{
                if("knock_server".equals(message)){
                    logAtList(ConfigModule.isFakeMode() ? "找不到长者":"找不到服务器");
                    JOptionPane.showMessageDialog(this,"找不到服务器，请检查网络设置",getTitle(),JOptionPane.WARNING_MESSAGE);
                }
                shuttleOffline();
                uiOffline();
            }break;

            case SOCKET_NO_ROUTE_TO_HOST:{
                logAtList(ConfigModule.isFakeMode() ? "续命不能" : "无路由到服务器");
                JOptionPane.showMessageDialog(this,"数据包不能路由到服务器，请检查网络设置",getTitle(),JOptionPane.WARNING_MESSAGE);
                shuttleOffline();
            }

            case SOCKET_UNKNOWN_HOST_EXCEPTION:{
                logAtList(ConfigModule.isFakeMode() ? "找不到长者":"找不到服务器");
                JOptionPane.showMessageDialog(this,"找不到服务器，请检查网络设置",getTitle(),JOptionPane.WARNING_MESSAGE);
                shuttleOffline();
            }break;

            case MESSAGE_EXCEPTION:{
                logAtList("消息线程遇到错误: " + message);
            }break;

            case MESSAGE_CLOSE:{
                logAtList("消息线程关闭");
            }break;

            //其他错误
            case SOCKET_OTHER_EXCEPTION:{
                if("knocking".equals(message)){
                    uiOffline();
                    shuttleOffline();
                    trayPopMessage(getTitle(),"敲门不成功，上线失败");
                }
                logAtList("错误:" + message);
            }break;

            //下线
            case OFFLINE:{
                if("generally".equals(message)){
                    logAtList(ConfigModule.isFakeMode() ? "不续命了" : "下线了");
                    trayPopMessage(getTitle(),"下线了");
                }else if("timeout".equals(message)){
                    logAtList(ConfigModule.isFakeMode() ? "不续命了,不过好像长者没听到" : "下线超时,但已经下线了");
                    trayPopMessage(getTitle(),"下线了");
                }else{
                    logAtList(ConfigModule.isFakeMode() ? "不续命了!" : "暴力下线");
                    trayPopMessage(getTitle(),"粗暴地下线了");
                }
                uiOffline();
            }break;

            //续命成功（雾
            case BREATHE_SUCCESS:{
                try {
                    logAtList("[" + (new Date().toString()).split(" ")[3] + (ConfigModule.isFakeMode() ? "]续命成功":"]在线状态续期成功"));
                }catch (Exception e){
                    logAtList(ConfigModule.isFakeMode() ? "续命成功x20":"在线状态续期成功");
                    if(Program.isDeveloperMode()) logAtList("流水号" + String.format("0x%x",shuttle.getSerialNo()));
                }
                setOnlineIcon();
                stat_icon.setText("");
            }break;

            //续命失败（大雾
            case BREATHE_FAILED:{
                logAtList(ConfigModule.isFakeMode() ? "图样图森破":"服务器否认在线状态");
                setOfflineIcon();
                shuttleOffline();
                uiOffline();
                stat_icon.setText("被下线");
            }break;

            //续命错误（超级雾
            case BREATHE_EXCEPTION:{
                //stat_icon.setText("");
                if("timeout".equals(message)){
                    logAtList(ConfigModule.isFakeMode() ? "续命超时，再试":"续期超时，重试");
                    setLinkingIcon();
                    if(trayIcon != null){
                        trayIcon.setImage(tray_linking);
                    }
                }else if("time_clear".equals(message)){
                    logAtList(ConfigModule.isFakeMode() ? "重新给长者续命":"服务器要求在线时常复位");
                } else {
                    logAtList((ConfigModule.isFakeMode() ? "续命时遇到错误：":"呼吸进程遇到错误：") + message);
                    if(Toolbox.isMacOSX()){
                        JOptionPane.showMessageDialog(this,"呼吸进程遇到错误: " + message,getTitle(),JOptionPane.WARNING_MESSAGE);
                    }else{
                        trayPopMessage(getTitle(),"呼吸进程遇到错误，下线了");
                    }
                    shuttleOffline();
                    uiOffline();
                }
            }break;

            //消息线程接收到消息
            case SERVER_MESSAGE:{
                if("offline".equals(message)){
                    logAtList(ConfigModule.isFakeMode() ? "西方记者不干了":"服务器要求下线");
                    trayPopMessage(getTitle(), "服务器要求下线");
                    //stat_icon.setText("被下线");
                }else{
                    logAtList((ConfigModule.isFakeMode() ? "长者说:":"接收到了一条服务器消息:")+message);
                    trayPopMessage("接收到了一条服务器消息",message);
                }
            }break;
        }
    }

    @Override
    public void onNetworkError(int errorType, String message) {

    }

    @Override
    public void windowOpened(WindowEvent e) {
        logger.trace("Window Opened");
        if(Program.isDeveloperMode()){
            JOptionPane.showMessageDialog(null, "你开启了开发者模式\n" +
                    "请注意，开发者模式将记录你所有的用户使用信息（包括账号密码）\n" +
                    "请慎用开发者模式，如有需要请清理日志文件以保护隐私。", "Developer Mode on", JOptionPane.WARNING_MESSAGE, icon_dev);
        }
    }

    @Override
    public void windowClosing(WindowEvent e) {
        if (!ConfigModule.hideOnClose && ConfigModule.autoSaveSetting) {
            ConfigModule.windowWidth = getWidth();
            ConfigModule.windowHeight = getHeight();
            ConfigModule.writeProfile();
        }
    }

    @Override
    public void windowClosed(WindowEvent e) {
    }

    @Override
    public void windowIconified(WindowEvent e) {
        if(ConfigModule.hideOnIconified && !Toolbox.isMacOSX()) setVisible(false);
    }

    @Override
    public void windowDeiconified(WindowEvent e) {
        setVisible(true);
    }

    @Override
    public void windowActivated(WindowEvent e) {
        if(Toolbox.isMacOSX()) com.apple.eawt.Application.getApplication().requestUserAttention(false);
    }

    @Override
    public void windowDeactivated(WindowEvent e) {

    }

    @Override
    public void mouseClicked(MouseEvent e) {
        if(e.getSource() == trayIcon){
            if(Toolbox.isMacOSX()){
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
