package com.shinonometn.Loom.ui;

import com.shinonometn.Loom.Program;
import com.shinonometn.Loom.common.ConfigModule;
import com.shinonometn.Loom.common.Logger;
import com.shinonometn.Loom.common.Networks;
import com.shinonometn.Loom.connector.Messenger.ShuttleEvent;
import com.shinonometn.Loom.connector.Shuttle;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.net.NetworkInterface;
import java.util.*;

/**
 * Created by catten on 15/10/20.
 */
public class MainForm extends JFrame implements ActionListener,ShuttleEvent,WindowListener{

    JTextField t_username;
    JPasswordField t_password;

    JLabel stat_icon;

    JButton btn_login;
    JCheckBox cb_remember;
    JCheckBox cb_Log;
    JCheckBox cb_printLog;

    JComboBox<String> cb_netcard;

    JList<String> list1;
    DefaultListModel<String> listModel = new DefaultListModel<String>();

    JMenuBar menuBar;

    JMenu menuOptions;
    JMenuItem menuItemCleanLogs;
    JMenuItem menuItemSaveProfile;

    JMenu menuAbout;
    JMenuItem menuItemAbout;
    JMenuItem menuItemHelp;

    Shuttle shuttle;
    Vector<NetworkInterface> nf;

    ImageIcon icon_online = new ImageIcon(getClass().getResource("/com/shinonometn/img/link.png"));
    ImageIcon icon_offline = new ImageIcon(getClass().getResource("/com/shinonometn/img/link_break.png"));
    ImageIcon icon_linking = new ImageIcon(getClass().getResource("/com/shinonometn/img/link_go.png"));
    ImageIcon icon_app = new ImageIcon(getClass().getResource("/com/shinonometn/img/package_link.png"));
    ImageIcon icon_dev = new ImageIcon(getClass().getResource("/com/shinonometn/img/bomb.png"));

    public MainForm(){
        super("Loom");
        setMinimumSize(new Dimension(200, 400));
        setSize(ConfigModule.windowWidth, ConfigModule.windowHeight);
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setupUI();
        setVisible(true);
        setupEvent();

        //ConfigModule.readProfiles();
    }

    private void setupUI(){
        menuBar = new JMenuBar();
        setJMenuBar(menuBar);

        menuOptions = new JMenu("选项");
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
        menuItemCleanLogs = new JMenuItem("清除日志目录");
        menuItemSaveProfile = new JMenuItem("立即保存设置");
        //-
        menuOptions.add(cb_Log);
        menuOptions.add(cb_printLog);
        menuOptions.add(new JPopupMenu.Separator());
        menuOptions.add(cb_remember);
        menuOptions.add(new JPopupMenu.Separator());
        menuOptions.add(menuItemCleanLogs);
        menuOptions.add(menuItemSaveProfile);

        menuAbout = new JMenu("关于");
        menuItemHelp = new JMenuItem("帮助");
        menuItemHelp.addActionListener(this);
        menuItemAbout = new JMenuItem("关于软件");
        menuItemAbout.addActionListener(this);

        menuAbout.add(menuItemHelp);
        menuAbout.add(new JPopupMenu.Separator());
        menuAbout.add(menuItemAbout);
        menuAbout.add(new JPopupMenu.Separator());
        JMenuItem m1;
        m1 = new JMenuItem("Loom v1.0");
        m1.setEnabled(false);
        menuAbout.add(m1);
        m1 = new JMenuItem("Pupa version:3.6");
        m1.setEnabled(false);
        menuAbout.add(m1);
        m1 = new JMenuItem("Amnoon Auth. v3.6.9");
        m1.setEnabled(false);
        menuAbout.add(m1);

        menuBar.add(menuOptions);
        menuBar.add(menuAbout);

        setLayout(new GridBagLayout());
        GridBagConstraints gridBagConstraints = new GridBagConstraints();
        Insets left_inset = new Insets(2,10,2,0);
        Insets right_inset = new Insets(2,0,2,10);
        Insets normal_inset = new Insets(2,2,2,2);

        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.weightx = 0;
        gridBagConstraints.insets = left_inset;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        add(new JLabel("用户名",JLabel.RIGHT),gridBagConstraints);

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
        add(new JLabel("密码",JLabel.RIGHT),gridBagConstraints);

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
        add(new JLabel("网卡",JLabel.RIGHT),gridBagConstraints);

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

        //检查一下有没有可用网卡，没有则不给操作
        if(nf == null ||nf.size() <= 0){
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

    //锁定输入UI
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

    //解锁输入UI
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

    private void uiOffline(){
        btn_login.setText("上线");
        cb_netcard.setEnabled(true);
        unlockInputUI();
        stat_icon.setIcon(icon_offline);
    }

    //事件监听设置
    private void setupEvent(){
        addWindowListener(this);

        cb_remember.addActionListener(this);
        btn_login.addActionListener(this);
        cb_Log.addActionListener(this);
        cb_remember.addActionListener(this);
        cb_printLog.addActionListener(this);
        menuItemCleanLogs.addActionListener(this);
        menuItemSaveProfile.addActionListener(this);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        //登录按钮的动作
        if(e.getSource() == btn_login){

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
        }else if(e.getSource() == cb_Log){//设置是否启动log

            ConfigModule.useLog = !ConfigModule.useLog;
            cb_Log.setSelected(ConfigModule.useLog);
        }else if(e.getSource() == menuItemCleanLogs){//清理日志

            Logger.clearLog();
        }else if(e.getSource() == menuItemSaveProfile){//立即保存配置

            applyProfile();
            ConfigModule.writeProfile();
        }else if(e.getSource() == cb_printLog){

            ConfigModule.outPrintLog = cb_printLog.isSelected();
        }
        if(ConfigModule.autoSaveSetting) applyProfile();
    }

    private void applyProfile(){
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
        listModel.add(listModel.getSize(),s);
    }

    @Override
    public void onMessage(int messageType, String message) {
        switch (messageType){
            //服务器无响应
            case SHUTTLE_SERVER_NO_RESPONSE:{
                if("knock_server_no_response".equals(message)){
                    unlockInputUI();
                    btn_login.setText("上线");
                    logAtList("获取认证服务器失败:服务器无响应");
                    JOptionPane.showMessageDialog(
                            this,
                            "敲门无响应\n请检查您所选择的网卡是否已连接到校园网",
                            this.getTitle(),
                            JOptionPane.WARNING_MESSAGE
                    );
                    shuttle.dispose();
                    shuttle = null;
                }else if("certificate_timeout".equals(message)){
                    uiOffline();
                    logAtList("认证超时，服务器无响应");
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

            //认证成功
            case SHUTTLE_CERTIFICATE_SUCCESS:{
                logAtList("认证成功");
                logAtList("已上线");
                btn_login.setEnabled(true);
                btn_login.setText("下线");
                stat_icon.setIcon(icon_online);
            }break;

            //认证失败
            case SHUTTLE_CERTIFICATE_FAILED:{
                logAtList("认证失败:"+message);
                btn_login.setEnabled(true);
                btn_login.setText("上线");
                unlockInputUI();
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
                    shuttle = null;
                }else if("get_message_socket_failed".equals(message)){
                    logAtList("消息监听端口被占用，获取Socket失败");
                    JOptionPane.showMessageDialog(
                            this,
                            "尝试监听服务器消息失败，可能端口正在被占用。\n不监听服务器消息就无法得知什么时候断网，不过不影响上线。",
                            getTitle(),
                            JOptionPane.INFORMATION_MESSAGE
                    );
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
            }break;

            //其他错误
            case SHUTTLE_OTHER_EXCEPTION:{
                if("unknown_exception_knocking".equals(message)){
                    uiOffline();
                    shuttle.dispose();
                    shuttle = null;
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
                stat_icon.setIcon(icon_online);
            }break;

            //续命失败（大雾
            case SHUTTLE_BREATHE_FAILED:{
                logAtList("服务器否认在线状态");
                stat_icon.setIcon(icon_offline);
            }break;

            //续命错误（超级雾
            case SHUTTLE_BREATHE_EXCEPTION:{
                if("breathe_timeout".equals(message)){
                    logAtList("续期超时，重试");
                    stat_icon.setIcon(icon_linking);
                }else if("breathe_time_clear".equals(message)){
                    logAtList("服务器要求在线时常复位");
                }
                else logAtList("呼吸进程遇到错误：" + message);
                //stat_icon.setIcon(icon_offline);
            }break;

            //消息线程接收到消息
            case SHUTTLE_SERVER_MESSAGE:{
                if("offline".equals(message)){
                    logAtList("服务器要求下线");
                }else{
                    logAtList("接收到了一条服务器消息:"+message);
                }
            }break;
        }
    }

    @Override
    public void windowOpened(WindowEvent e) {
        Logger.log("Window Opened");
        if(Program.isDeveloperMode()){
            JOptionPane.showMessageDialog(null,"你开启了开发者模式\n" +
                    "请注意，开发者模式将记录你所有的用户使用信息（包括账号密码）\n" +
                    "请慎用开发者模式，如有需要请清理日志文件以保护隐私。","Developer Mode on",JOptionPane.WARNING_MESSAGE,icon_dev);
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
    }

    @Override
    public void windowDeiconified(WindowEvent e) {
        Logger.log("Window Deiconified");
    }

    @Override
    public void windowActivated(WindowEvent e) {
        Logger.log("Window Activated");
    }

    @Override
    public void windowDeactivated(WindowEvent e) {
        Logger.log("Window Deactivated");
    }
}
