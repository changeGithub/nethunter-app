package com.offsec.nethunter;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;

import java.io.File;

public class KaliServicesFragment extends Fragment {
    /**
     * The fragment argument representing the section number for this
     * fragment.
     */
    private String[][] KaliServices;
    private static final String ARG_SECTION_NUMBER = "section_number";
    boolean updateStatuses = false;

    public KaliServicesFragment() {

    }


    /**
     * Returns a new instance of this fragment for the given section
     * number.
     */
    public static KaliServicesFragment newInstance(int sectionNumber) {
        KaliServicesFragment fragment = new KaliServicesFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_SECTION_NUMBER, sectionNumber);
        fragment.setArguments(args);
        return fragment;
    }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.kali_services, container, false);
        checkServices(rootView);
        return rootView;

    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        if (isAdded()) {
            String fileDir = getActivity().getFilesDir().toString() + "/scripts";

            KaliServices = new String[][]{

                    // {name, check_cmd, start_cmd, stop_cmd, init_service_filename}

                    {"SSH", "sh " + fileDir + "/check-kalissh", "su -c '" + fileDir + "/bootkali ssh start'", "su -c '" + fileDir + "/bootkali ssh stop'", "70ssh"},
                    {"Dnsmasq", "sh " + fileDir + "/check-kalidnsmq", "su -c '" + fileDir + "/bootkali dnsmasq start'", "su -c '" + fileDir + "/bootkali dnsmasq stop'", "70dnsmasq"},
                    {"Hostapd", "sh " + fileDir + "/check-kalihostapd", "su -c '" + fileDir + "/bootkali hostapd start'", "su -c '" + fileDir + "/bootkali hostapd stop'", "70hostapd"},
                    {"OpenVPN", "sh " + fileDir + "/check-kalivpn", "su -c '" + fileDir + "/bootkali openvpn start'", "su -c '" + fileDir + "/bootkali openvpn stop'", "70openvpn"},
                    {"Apache", "sh " + fileDir + "/check-kaliapache", "su -c '" + fileDir + "/bootkali apache start'", "su -c '" + fileDir + "/bootkali apache stop'", "70apache"},
                    {"Metasploit", "sh " + fileDir + "/check-kalimetasploit", "su -c '" + fileDir + "/bootkali msf start'", "su -c '" + fileDir + "/bootkali msf stop'", "70msf"},
                    //{"DHCP", "sh " + fileDir + "/check-kalidhcp","su -c '" + cachedir + "/bootkali dhcp start'","su -c '" + cachedir + "/bootkali dhcp stop'", "70dhcp"},
                    {"BeEF Framework", "sh " + fileDir + "/check-kalibeef-xss", "su -c '" + fileDir + "/bootkali beef-xss start'", "su -c '" + fileDir + "/bootkali beef-xss stop'", "70beef"},
                    {"Y-cable Charging", "sh " + fileDir + "/check-ycable","su -c 'bootkali ycable start'","su -c 'bootkali ycable stop'", "70ycable"},
                    //{"Fruity WiFi", "sh " + fileDir + "/check-fruity-wifi","su -c start-fruity-wifi","su -c  stop-fruity-wifi", "70fruity"}
                    // the stop script isnt working well, doing a raw cmd instead to stop vnc
                    // {"VNC", "sh " + fileDir + "/check-kalivnc", "" + cachedir + "/bootkali\nvncserver", "" + cachedir + "/bootkali\nkill $(ps aux | grep 'Xtightvnc' | awk '{print $2}');CT=0;for x in $(ps aux | grep 'Xtightvnc' | awk '{print $2}'); do CT=$[$CT +1];tightvncserver -kill :$CT; done;rm /root/.vnc/*.log;rm -r /tmp/.X*", "70vnc"},
            };
        }
    }

    public void onResume()
    {
        super.onResume();
        updateStatuses = true;
    }

    public void onPause()
    {
        super.onPause();
        updateStatuses = false;
    }

    public void onStop()
    {
        super.onStop();
        updateStatuses = false;
    }


    private void checkServices(final View rootView) {

        new Thread(new Runnable() {

            public void run() {

                ShellExecuter exe = new ShellExecuter();
                final ListView servicesList = (ListView) rootView.findViewById(R.id.servicesList);
                String checkCmd = "";
                String checkBootStates = "";
                String bootScriptPath = getActivity().getFilesDir().toString() + "/etc/init.d/";
                for (String[] KaliService : KaliServices) {
                    File checkBootFile = new File(bootScriptPath + KaliService[4]);
                    if (checkBootFile.exists()) {
                        checkBootStates += "1";
                    } else {
                        checkBootStates += "0";
                    }
                    checkCmd += KaliService[1] + ";";
                }

                final String serviceStates = exe.RunAsRootOutput(checkCmd);
                final String finalCheckBootStates = checkBootStates;
                servicesList.post(new Runnable() {
                    @Override
                    public void run() {
                        servicesList.setAdapter(new KaliServicesLoader(getActivity().getApplicationContext(), serviceStates, finalCheckBootStates, KaliServices));
                    }
                });

            }
        }).start();
    }

}


// This class is the main for the services


class KaliServicesLoader extends BaseAdapter {

    private Context mContext;
    private String[] _serviceStates;
    private String[] _serviceBootStates;
    private String services[][];
    private String bootScriptPath;
    private String shebang;
    private ShellExecuter exe = new ShellExecuter();


    public KaliServicesLoader(Context context, String serviceStates, String bootStates, String[][] KaliServices) {

        mContext = context;

        services = KaliServices;
        _serviceStates = serviceStates.split("(?!^)");
        _serviceBootStates = bootStates.split("(?!^)");

        bootScriptPath = mContext.getFilesDir().toString() + "/etc/init.d/";
        shebang = "#!/system/bin/sh\n\n# Init at boot kaliSevice: ";

    }

    static class ViewHolderItem {
        // The switch
        Switch sw;
        // the msg holder
        TextView swholder;
        // the service title
        TextView swTitle;
        // run at boot checkbox
        CheckBox swBootCheckbox;
    }

    public int getCount() {
        // return the number of services
        return services.length;
    }
    public void addBootService(int serviceId) {
        String bootServiceFile = bootScriptPath + services[serviceId][4];
        String fileContents = shebang + services[serviceId][0] + "\n" + services[serviceId][2];
        exe.RunAsRoot(new String[]{
            "echo '"+ fileContents +"' > " +  bootServiceFile,
            "chmod 700 " + bootServiceFile
        });

        // return the number of services

    }
    public void removeBootService(int serviceId) {
        // return the number of services
        String bootServiceFile = bootScriptPath + services[serviceId][4];
        exe.RunAsRoot(new String[]{ "rm -rf " +  bootServiceFile });
    }
    // getView method is called for each item of ListView
    public View getView(final int position, View convertView, ViewGroup parent) {
        // inflate the layout for each item of listView (our services)

        ViewHolderItem vH;

        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.kali_services_item, parent, false);

            // set up the ViewHolder
            vH = new ViewHolderItem();
            // get the reference of switch and the text view
            vH.swTitle = (TextView) convertView.findViewById(R.id.switchTitle);
            vH.sw = (Switch) convertView.findViewById(R.id.switch1);
            vH.swholder = (TextView) convertView.findViewById(R.id.switchHolder);
            vH.swBootCheckbox = (CheckBox) convertView.findViewById(R.id.initAtBoot);
            convertView.setTag(vH);
            //System.out.println ("created row");
        } else {
            // recycle the items in the list is already exists
            vH = (ViewHolderItem) convertView.getTag();
        }
        if (position >= _serviceStates.length) {
            // out of range, return ,do nothing
            return convertView;
        }
        // remove listeners
        vH.sw.setOnCheckedChangeListener(null);
        vH.swBootCheckbox.setOnCheckedChangeListener(null);
        // set service name
        vH.swTitle.setText(services[position][0]);
        // clear state
        vH.sw.setChecked(false);
        vH.swBootCheckbox.setChecked(false);
        // check it

        // running services
        if (_serviceStates[position].equals("1")) {
            vH.sw.setChecked(true);
            vH.swTitle.setTextColor(mContext.getResources().getColor(R.color.blue));
            vH.swholder.setText(services[position][0] + " Service is UP");
            vH.swholder.setTextColor(mContext.getResources().getColor(R.color.blue));
        } else {
            vH.sw.setChecked(false);

            vH.swTitle.setTextColor(mContext.getResources().getColor(R.color.clearTitle));
            vH.swholder.setText(services[position][0] + " Service is DOWN");
            vH.swholder.setTextColor(mContext.getResources().getColor(R.color.clearText));
        }
        // services enabled at boot
        if (_serviceBootStates[position].equals("1")) {
            // is enabled
            vH.swBootCheckbox.setChecked(true);
            vH.swBootCheckbox.setTextColor(mContext.getResources().getColor(R.color.blue));
        } else {
            // is not :)
            vH.swBootCheckbox.setChecked(false);
            vH.swBootCheckbox.setTextColor(mContext.getResources().getColor(R.color.clearTitle));
        }

        // add listeners
        final ViewHolderItem finalVH = vH;
        vH.sw.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    new Thread(new Runnable() {
                        public void run() {
                            exe.RunAsRoot(new String[]{services[position][2]});
                        }

                    }).start();
                    _serviceStates[position] = "1";
                    finalVH.swholder.setText(services[position][0] + " Service Started");
                    finalVH.swTitle.setTextColor(mContext.getResources().getColor(R.color.blue));
                    finalVH.swholder.setTextColor(mContext.getResources().getColor(R.color.blue));

                } else {
                    new Thread(new Runnable() {
                        public void run() {
                            exe.RunAsRoot(new String[]{services[position][3]});
                        }

                    }).start();
                    _serviceStates[position] = "0";
                    finalVH.swholder.setText(services[position][0] + " Service Stopped");
                    finalVH.swTitle.setTextColor(mContext.getResources().getColor(R.color.clearTitle));
                    finalVH.swholder.setTextColor(mContext.getResources().getColor(R.color.clearText));

                }
            }
        });
        vH.swBootCheckbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    new Thread(new Runnable() {
                        public void run() {
                            Log.d("bootservice","ADD " + services[position][4]);
                            addBootService(position);
                        }
                    }).start();
                    _serviceBootStates[position] = "1";
                    finalVH.swBootCheckbox.setTextColor(mContext.getResources().getColor(R.color.blue));
                } else {
                    new Thread(new Runnable() {
                        public void run() {
                            Log.d("bootservice", "REMOVE " + services[position][4]);
                            removeBootService(position);
                        }
                    }).start();
                    _serviceBootStates[position] = "0";
                    finalVH.swBootCheckbox.setTextColor(mContext.getResources().getColor(R.color.clearTitle));

                }
            }
        });
        return convertView;
    }

    public String[] getItem(int position) {

        return services[position];
    }

    public long getItemId(int position) {

        return position;
    }
}