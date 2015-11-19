package com.shinonometn.Loom.ui;

import com.shinonometn.Loom.Program;
import com.shinonometn.Loom.common.ConfigModule;
import com.shinonometn.Loom.common.Logger;
import com.shinonometn.Loom.common.Networks;
import com.shinonometn.Loom.common.Toolbox;
import com.shinonometn.Loom.connector.Messenger.ShuttleEvent;
import com.shinonometn.Loom.connector.Shuttle;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.*;
import java.net.NetworkInterface;
import java.util.*;



/**
 * Created by catten on 15/10/20.
 */
public class MainForm extends JFrame implements ActionListener,ShuttleEvent,WindowListener, MouseListener{

    JTextField t_username;
    JPasswordField t_password;

    JLabel stat_icon;

    JButton btn_login;

    JCheckBox cb_remember;
    JCheckBox cb_hideOnIconfied;
    JCheckBox cb_notShownAtLaunch;

    JComboBox<String> cb_netcard;

    JList<String> list1;
    JScrollPane scrollPane;
    DefaultListModel<String> listModel;

    JMenuBar menuBar;

    //JMenu menuOptions;
    JMenu menuLogs;
    JCheckBox cb_showInfo;
    JCheckBox cb_Log;
    JCheckBox cb_printLog;
    JMenuItem menuItemCleanInfos;
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

//图标资源
    ImageIcon icon_online = new ImageIcon(getClass().getResource("/com/shinonometn/img/link.png"));
    ImageIcon icon_offline = new ImageIcon(getClass().getResource("/com/shinonometn/img/link_break.png"));
    ImageIcon icon_linking = new ImageIcon(getClass().getResource("/com/shinonometn/img/link_go.png"));
    ImageIcon icon_app = new ImageIcon(getClass().getResource("/com/shinonometn/img/package_link.png"));
    ImageIcon icon_dev = new ImageIcon(getClass().getResource("/com/shinonometn/img/bomb.png"));

    Image tray_online = Toolkit.getDefaultToolkit().getImage(getClass().getResource("/com/shinonometn/img/package_link 2.png"));
    Image tray_linking = Toolkit.getDefaultToolkit().getImage(getClass().getResource("/com/shinonometn/img/package_go.png"));
    Image tray_offline = Toolkit.getDefaultToolkit().getImage(getClass().getResource("/com/shinonometn/img/package.png"));
    Image image_app = Toolkit.getDefaultToolkit().getImage(getClass().getResource("/com/shinonometn/img/package_link.png"));
//--------

    public MainForm(){
        super("Loom");
        setMinimumSize(new Dimension(200, 180));
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
        setJMenuBar(menuBar);

        //menuOptions = new JMenu("选项");
        //-
        cb_Log = new JCheckBox("启用日志");
        cb_Log.setSelected(ConfigModule.useLog);
        //-
        cb_remember = new JCheckBox("自动保存设置");
        cb_remember.setSelected(ConfigModule.autoSaveSetting);
        //-
        cb_printLog = new JCheckBox("输出日志到终端");
        cb_printLog.setSelected(ConfigModule.outPrintLog);
        //cb_printLog.setVisible(Program.isDeveloperMode());
        //-
        cb_hideOnIconfied = new JCheckBox("最小化时隐藏");
        cb_hideOnIconfied.setSelected(ConfigModule.hideOnIconified);
        //-
        cb_showInfo = new JCheckBox("显示连接信息");
        cb_showInfo.setSelected(ConfigModule.showInfo);
        //-
        cb_notShownAtLaunch = new JCheckBox("启动时不显示窗体");
        cb_notShownAtLaunch.setSelected(ConfigModule.notShownAtLaunch);
        //-
        menuItemCleanLogs = new JMenuItem("清除日志目录");
        menuItemSaveProfile = new JMenuItem("立即保存设置");
        menuItemCleanInfos = new JMenuItem("清除链接信息");
        menuItemCleanInfos.setEnabled(ConfigModule.showInfo);
        //-
        menuItemState = new MenuItem("状态：下线");
        menuItemState.setEnabled(false);
        menuItemOnline = new MenuItem("上线");
        menuItemExit = new MenuItem("退出");

        JMenu menu = new JMenu("设置");
        menu.add(cb_remember);
        menu.add(cb_hideOnIconfied);
        menu.add(cb_notShownAtLaunch);
        menu.add(cb_showInfo);
        menu.add(new JPopupMenu.Separator());
        menu.add(menuItemSaveProfile);
        menuBar.add(menu);

        menuLogs = new JMenu("日志");
        menuLogs.add(cb_Log);
        menuLogs.add(cb_printLog);
        menuLogs.add(cb_showInfo);
        menuLogs.add(new JPopupMenu.Separator());
        menuLogs.add(menuItemCleanInfos);
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
        m1 = new JMenuItem("Loom v1.0");
        m1.setEnabled(false);
        menu.add(m1);
        m1 = new JMenuItem("Pupa version:3.6");
        m1.setEnabled(false);
        menu.add(m1);
        m1 = new JMenuItem("Amnoon Auth. v3.6.9");
        m1.setEnabled(false);
        menu.add(m1);
        menuBar.add(menu);


        setLayout(new GridBagLayout());
        GridBagConstraints gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.anchor = GridBagConstraints.BELOW_BASELINE_LEADING;
        Insets left_inset = new Insets(2,10,2,0);
        Insets right_inset = new Insets(2,0,2,10);
        Insets normal_inset = new Insets(2,2,2,2);

        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.weightx = 0;
        gridBagConstraints.insets = left_inset;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        add(new JLabel("用户名:",JLabel.RIGHT),gridBagConstraints);

        gridBagConstraints.gridx++;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.weightx = 0.5;
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
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.insets = right_inset;
        t_password = new JPasswordField();
        t_password.setText(ConfigModule.password);
        add(t_password, gridBagConstraints);

        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy++;
        gridBagConstraints.insets = left_inset;
        gridBagConstraints.gridwidth = 1;
        add(new JLabel("网卡:",JLabel.RIGHT),gridBagConstraints);

        gridBagConstraints.gridx++;
        gridBagConstraints.gridwidth = 2;
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
        gridBagConstraints.weightx = 0.2;
        stat_icon = new JLabel(icon_offline,JLabel.CENTER);
        add(stat_icon,gridBagConstraints);

        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.insets = right_inset;
        //gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        btn_login = new JButton("上线");
        add(btn_login, gridBagConstraints);

        gridBagConstraints.gridy++;
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridwidth = 3;
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

        btn_login.addActionListener(this);

        cb_remember.addActionListener(this);
        cb_Log.addActionListener(this);
        cb_remember.addActionListener(this);
        cb_printLog.addActionListener(this);
        cb_hideOnIconfied.addActionListener(this);
        cb_notShownAtLaunch.addActionListener(this);
        cb_showInfo.addActionListener(this);

        menuItemCleanLogs.addActionListener(this);
        menuItemSaveProfile.addActionListener(this);
        menuItemAbout.addActionListener(this);
        menuItemHelp.addActionListener(this);
        menuItemCleanInfos.addActionListener(this);
        menuItemOnline.addActionListener(this);
        menuItemExit.addActionListener(this);

        //cb_hideOnIconfied.addChangeListener(this);
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
            }
        }
    }

//--------------------------------------------------------

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
    private void uiOffline(){
        btn_login.setText("上线");
        //cb_netcard.setEnabled(true);
        menuItemOnline.setLabel(btn_login.getText());
        unlockInputUI();
        setOfflineIcon();
    }

    private void shuttleOffline(){
        if(shuttle != null){
            shuttle.dispose();
            shuttle = null;
        }
    }


    @Override
    public void actionPerformed(ActionEvent e) {
        //登录按钮的动作
        if(e.getSource() == btn_login || e.getSource() == menuItemOnline){

            if (shuttle != null && shuttle.isOnline()) {
                shuttle.Offline();
                lockInputUI();
                btn_login.setText("下线中");
            }else{
                shuttle = new Shuttle(nf.get(cb_netcard.getSelectedIndex()),this);
                shuttle.developerMode = Program.isDeveloperMode();
                shuttle.setUsername(t_username.getText());
                shuttle.setPassword(new String(t_password.getPassword()));
                shuttle.start();
                ConfigModule.username = t_username.getText();
                ConfigModule.password = new String(t_password.getPassword());
                lockInputUI();
                btn_login.setText("上线中...");
            }
            menuItemOnline.setLabel(btn_login.getText());
        }else if(e.getSource() == menuItemAbout){ //菜单里的关于

            String aboutInfo = "Loom\t(不是真的纺纱机啦)" +
                    "\n您可以无偿使用这个软件，用以登录岭南职院校园网。" +
                    "\n\n此软件开源自由，遵循GPL(General Public License)协议" +
                    "\nhttp://www.gnu.org/licenses/gpl.html" +
                    "\n源代码托管于Github上" +
                    "\n有关于此软件出现的问题可以邮件至我邮箱：" +
                    "\nkozakcuu@gmail.com";

            JOptionPane.showMessageDialog(null,aboutInfo,"关于 Loom",JOptionPane.INFORMATION_MESSAGE,icon_app);
        }else if(e.getSource() == menuItemHelp){//菜单里的帮助

            String helpInfo = "欢迎使用Loom" +
                    "\n\n填写好登录账号和密码之后，选择已经链接到校园网的网卡，然后上线。" +
                    "\n如果提示没有可用网卡，请检查网线或者WLAN是否已经连接上" +
                    "\n如果需要使用WLAN（无线网卡）连接校园网，请把无线路由器调至交换机模式" +
                    "\n软件本身不能让你无限时长上网" +
                    "\n如果认证超时，重试即可。如依旧不能解决，请到 http://172.19.1.8/selfLogoutAction.do 登陆后强制下线" +
                    "\n更多问题请自己探索或发邮件于我。";
            JOptionPane.showMessageDialog(null,helpInfo,"帮助",JOptionPane.INFORMATION_MESSAGE,icon_app);
        }else if(e.getSource() == cb_remember){ //保存用户设置

            ConfigModule.autoSaveSetting = cb_remember.isSelected();
            if(ConfigModule.autoSaveSetting) ConfigModule.writeProfile();
        }else if(e.getSource() == cb_Log){ //设置是否启动log

            ConfigModule.useLog = !ConfigModule.useLog;
            cb_Log.setSelected(ConfigModule.useLog);
            if(Program.isDeveloperMode() && ConfigModule.useLog){
                trayPopMessage("Developer Mode On","您开启了开发者模式，您的账号信息有可能会被记录到日志文件内。");
            }
        }else if(e.getSource() == menuItemCleanLogs){ //清理日志

            int count;
            if((count = Logger.clearLog()) >= 0){
                JOptionPane.showMessageDialog(this,count +" 个日志已清理.",getTitle(),JOptionPane.INFORMATION_MESSAGE);
            }else{
                JOptionPane.showMessageDialog(this,"清理日志失败",getTitle(),JOptionPane.WARNING_MESSAGE);
            }
        }else if(e.getSource() == menuItemSaveProfile){ //立即保存配置

            applyProfile();
            ConfigModule.writeProfile();
            trayPopMessage(getTitle(),"日志已保存");
        }else if(e.getSource() == cb_printLog){

            ConfigModule.outPrintLog = cb_printLog.isSelected();
        }else if(e.getSource() == cb_hideOnIconfied){

            ConfigModule.hideOnIconified = cb_hideOnIconfied.isSelected();
        }else if(e.getSource() == cb_notShownAtLaunch){

            ConfigModule.notShownAtLaunch = cb_notShownAtLaunch.isSelected();
        }else if(e.getSource() == cb_showInfo){

            ConfigModule.showInfo = cb_showInfo.isSelected();
            menuItemCleanInfos.setEnabled(ConfigModule.showInfo);
            scrollPane.setVisible(ConfigModule.showInfo);
            if(ConfigModule.showInfo && getHeight() < 240){
                setSize(getWidth(),240);
            }else if(!ConfigModule.showInfo){
                setSize(getWidth(),(getHeight() < getMinimumSize().height?getMinimumSize().height:ConfigModule.windowHeight));
            }
        }else if(e.getSource() == menuItemCleanInfos){
            listModel.clear();
        }else if(e.getSource() == menuItemExit){
            if(shuttle != null){
                int result = JOptionPane.showConfirmDialog(null,"是否先下线再退出？",getTitle(),JOptionPane.YES_NO_CANCEL_OPTION);
                switch (result){
                    case JOptionPane.YES_OPTION:
                        shuttleOffline();
                        while (shuttle.isOnline());
                        dispose();
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
        }else if(e.getSource() == cb_notShownAtLaunch){
            ConfigModule.notShownAtLaunch = cb_notShownAtLaunch.isSelected();
        }

        if(ConfigModule.autoSaveSetting) applyProfile();
    }

    public void applyProfile(){
        ConfigModule.username = t_username.getText();
        ConfigModule.password = new String(t_password.getPassword());
        ConfigModule.windowWidth = getWidth();
        ConfigModule.windowHeight = getHeight();
        ConfigModule.outPrintLog = cb_printLog.isSelected();
        ConfigModule.defaultInterface = cb_netcard.getItemAt(cb_netcard.getSelectedIndex());
        ConfigModule.autoSaveSetting = cb_remember.isSelected();
        ConfigModule.useLog = cb_Log.isSelected();
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
                            "敲门无响应\n请检查您所选择的网卡是否已连接到校园网",
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
            }break;

            //认证失败
            case SHUTTLE_CERTIFICATE_FAILED:{
                logAtList("认证失败:" + message);
                unlockInputUI();
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
                    logAtList("目的网卡端口号正在被使用，获取Socket失败");
                    JOptionPane.showMessageDialog(
                            this,
                            "端口正在被使用：目的网卡拨号端口被占用\n请查看您是否已经启动了其他拨号器，或者尝试更换目的网卡",
                            this.getTitle(),
                            JOptionPane.WARNING_MESSAGE
                    );
                    uiOffline();
                    shuttleOffline();
                    trayPopMessage(getTitle(),"目的网卡端口号正在被使用，上线失败");
                }else if("get_message_socket_failed".equals(message)){
                    logAtList("消息监听端口被占用，获取Socket失败");
                    JOptionPane.showMessageDialog(
                            this,
                            "尝试监听服务器消息失败，可能端口正在被占用。\n不监听服务器消息就无法得知什么时候断网，不过不影响上线。",
                            getTitle(),
                            JOptionPane.INFORMATION_MESSAGE
                    );
                    trayPopMessage(getTitle(), "消息监听端口被占用，无法监听服务器消息了。");
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
            }break;

            //续命失败（大雾
            case SHUTTLE_BREATHE_FAILED:{
                logAtList("服务器否认在线状态");
                setOfflineIcon();
            }break;

            //续命错误（超级雾
            case SHUTTLE_BREATHE_EXCEPTION:{
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
                    trayPopMessage(getTitle(),"服务器要求下线");
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
        setVisible(!ConfigModule.hideOnIconified);
    }

    @Override
    public void windowDeiconified(WindowEvent e) {
        Logger.log("Window Deiconified");
        //setVisible(true);
    }

    @Override
    public void windowActivated(WindowEvent e) {
        Logger.log("Window Activated");
    }

    @Override
    public void windowDeactivated(WindowEvent e) {
        Logger.log("Window Deactivated");
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        if(e.getSource() == trayIcon && (e.getButton() == MouseEvent.BUTTON2 || e.getButton() == MouseEvent.BUTTON3)){
            setVisible(true);
            //setState(JFrame.NORMAL);
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
