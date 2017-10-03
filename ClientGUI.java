package multicast;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Map;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

/** 
* 
* @author Anar 
*/

public class ClientGUI extends JFrame implements ActionListener, ListSelectionListener {

    public JLabel messages;
    public JLabel members;
    public JTextArea area;
    public JList list;
    public JList glist;
    public JTextField enter_name;
    public JTextField enter_msg;
    public JButton join;
    public JButton leave;
    public JButton send_msg;
    public JButton exit;
    public JButton refresh;
    public JButton create;
    public JScrollPane scroll;
    public JScrollPane scroll2;
    public JScrollPane scroll3;
    public JButton new_group;
    public boolean connected;
    public boolean joined;
    public boolean end;
    public boolean msg;
    public boolean ex;
    public boolean crt;
    public boolean lst;
    public boolean fresh;
    public String response;
    public String username;
    public int index;
    
        public ClientGUI () 
        {
            setSize(new Dimension(600, 400));
            setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            setTitle("MultiChat Client");
            GridLayout grid = new GridLayout(7, 2, 30, 20);
            Container content = getContentPane(); 
            content.setLayout(new BorderLayout());
            content.setLayout(grid); 
            messages = new JLabel("Messages");
            members = new JLabel("Members");
            area = new JTextArea();
            list = new JList();  
            glist = new JList(); 
            enter_name = new JTextField("Username");
            enter_msg = new JTextField("Enter Message");
            join = new JButton("JOIN");
            leave = new JButton("LEAVE");
            send_msg= new JButton("SEND MESSAGE");
            exit = new JButton("EXIT");
            create = new JButton("CREATE A GROUP");
            refresh = new JButton("REFRESH GROUPS");
            connected = false;
            end = false;
            msg = false;
            ex = false;
            crt = false;
            lst = false;
            joined = false;
            fresh = false;
            index = 0;
            scroll = new JScrollPane(area);
            scroll2 = new JScrollPane(list);
            scroll3 = new JScrollPane(glist);
            new_group = new JButton("New group");
            add(messages);
            add(members);
            add(scroll);
            add(scroll2);
            add(scroll3);
            add(enter_name);
            add(join);
            add(leave);
            add(enter_msg);
            add(send_msg);
            add(exit);
            add(create);
            add(refresh);
            join.addActionListener(this);
            send_msg.addActionListener(this);
            exit.addActionListener(this);
            leave.addActionListener(this);
            create.addActionListener(this);
            refresh.addActionListener(this);
            glist.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            glist.addListSelectionListener(this);
     }

        @Override
        public void actionPerformed(ActionEvent e) 
        {
            
                if (e.getSource() == join)
                {
            synchronized(this) 
            {
                    username = enter_name.getText();
                    response = "JOIN ";
                    connected = true;
                    joined = true;
            notify();
                }
            }    
            if(e.getSource() == leave)
            {
                end = true;
            }  
            if(e.getSource() == refresh)
            {
                fresh = true;
            }
            if(e.getSource() == send_msg)
            {
            
                response = enter_msg.getText();
                msg = true;
            }  
            if(e.getSource() == exit)
            {
                ex = true;
            }
            synchronized(this) 
            {
                if (e.getSource() == create)
                {
                    crt = true;
                    notify();
                }
                
            }
        }
    
        public void displayMembers()
        {
            String []temp = new String[ClientMain.memList.size()];
            int i =0;
            for(Map.Entry<String,String> entry: ClientMain.memList.entrySet())
            {    
                temp[i] = entry.getKey();
                i++;
            }
            list.setListData(temp);
        }
    
        public void displayMsg(String msg)
        {
            area.append(msg);
        }

        public void disposeChat()
        {
            dispose();
        }
        
        public void displayGroups(){
            String []temp;
            if(ClientMain.groupList.isEmpty())
            {
                glist.setName("No groups, create one");
                temp = new String[1];
                temp[0] = "No groups, create one";
            }
            else {
                temp = new String[ClientMain.groupList.size()];
                int i =0;
                for(Map.Entry<String,String> entry: ClientMain.groupList.entrySet())
                {    
                    temp[i] = entry.getValue();
                    i++;
                }
            }    
            glist.setListData(temp);
        }
        
    @Override
         public void valueChanged(ListSelectionEvent e) 
    {
        
        if(!e.getValueIsAdjusting()) 
        {
          synchronized(this) 
            {
                index = glist.getSelectedIndex();
                lst = true;
                notify();
            } 
        }
    }
         
    public void setName(String s) 
    {
        setTitle(s);
    }
    
    public void setEmpty() 
    {
        area.setText(" ");
    }
}



