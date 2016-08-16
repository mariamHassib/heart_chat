package de.vonniebelschuetz.ble_heart_rate_test.adapter;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.drawable.GradientDrawable;
import android.preference.PreferenceManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.vonniebelschuetz.ble_heart_rate_test.R;
import de.vonniebelschuetz.ble_heart_rate_test.app.Utils;
import de.vonniebelschuetz.ble_heart_rate_test.model.Message;


public class ChatRoomThreadAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static String TAG = ChatRoomThreadAdapter.class.getSimpleName();

    private String userId;
    private int SELF = 100;
    private static String today;
    String ageAndresthr="";
    int userAge=26;
    int userRestPulse=70;

    private Context mContext;
    private ArrayList<Message> messageArrayList;

    private class ViewHolder extends RecyclerView.ViewHolder {
        TextView messageView, timestamp;

        ViewHolder(View view) {
            super(view);
            messageView = (TextView) itemView.findViewById(R.id.message);
            timestamp = (TextView) itemView.findViewById(R.id.timestamp);
        }
    }


    public ChatRoomThreadAdapter(Context mContext, ArrayList<Message> messageArrayList, String userId) {
        this.mContext = mContext;
        this.messageArrayList = messageArrayList;
        this.userId = userId;

        Calendar calendar = Calendar.getInstance();
        today = String.valueOf(calendar.get(Calendar.DAY_OF_MONTH));
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView;

        // view type is to identify where to render the chat message
        // left or right
        if (viewType == SELF) {
            // self message
            itemView = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.chat_item_self, parent, false);
        } else {
            // others message
            itemView = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.chat_item_other, parent, false);
        }


        return new ViewHolder(itemView);
    }


    @Override
    public int getItemViewType(int position) {
        Message message = messageArrayList.get(position);
        if (message.getUser().getId().equals(userId)) {
            return SELF;
        }

        return position;
    }

    @Override
    public void onBindViewHolder(final RecyclerView.ViewHolder holder, int position) {
        Message message = messageArrayList.get(position);

        //boolean isLatestAgeRHR = false;

        // Check if this message contains age and resting pulse as such text_age,pulse_
        String messageString = message.getMessage();
        String regexString = Pattern.quote("__Start__") + "(.*?)" + Pattern.quote("__End__");
        Pattern pattern = Pattern.compile(regexString);
        // text contains the full text that you want to extract data
        Matcher matcher = pattern.matcher(messageString);

        if (matcher.find()) {
            ageAndresthr = matcher.group(1); // Since (.*?) is capturing group 1
            Log.i(TAG, "age and rest hr are: " + ageAndresthr);
            message.setMessage(messageString.substring(0, messageString.length() - 21));
            Log.i(TAG, "message now is: " + message.getMessage());
            String [] output = ageAndresthr.split(",");
            String ageString=output[0] ;
            String pulseString=output[1];
            userAge=Integer.parseInt(ageString);
            userRestPulse=Integer.parseInt(pulseString);
            Log.i(TAG, "user age " + Integer.toString(userAge) + ", " + userId + ", " + Integer.toString(userRestPulse));

        }


        ((ViewHolder) holder).messageView.setText(message.getMessage());

        String timestamp = getTimeStamp(message.getCreatedAt());

        if (message.getUser().getName() != null)
            // TODO I18n
            timestamp = message.getUser().getName() + ", " + timestamp + " Uhr";

        ((ViewHolder) holder).timestamp.setText(timestamp);
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(mContext);
        //int resting_pulse = Integer.parseInt(preferences.getString(mContext.getString(R.string.pref_key_resting_pulse), "50"));
        int resting_pulse=userRestPulse;
        int max_hr=(int)(208-(0.7*userAge));
        int color = Utils.getColor(message.getHr(), resting_pulse,(int)(max_hr-(max_hr-(0.8*max_hr))) , 0.0, 0.35, true);
        colorMessage(holder, color);
    }

    private void colorMessage(RecyclerView.ViewHolder holder, int color) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(mContext);
        String mode = preferences.getString(mContext.getResources().getString(R.string.pref_key_display_mode_list), "numeric");
        if (mode.equals("history")) {
            GradientDrawable bgDrawable = (GradientDrawable) ((ViewHolder)holder).messageView.getBackground();
            bgDrawable.setColor(color);
            ((ViewHolder)holder).messageView.setBackground(bgDrawable);
        } else {
            GradientDrawable bgDrawable = (GradientDrawable) ((ViewHolder)holder).messageView.getBackground();
            if (holder.getItemViewType() == SELF) {
                // gray
                bgDrawable.setColor(mContext.getResources().getColor(R.color.bg_bubble_self));
            } else {
                bgDrawable.setColor(mContext.getResources().getColor(R.color.bg_bubble_other));
            }
            ((ViewHolder)holder).messageView.setBackground(bgDrawable);
        }
    }

    @Override
    public int getItemCount() {
        return messageArrayList.size();
    }

    public int getUserAge() {
        return userAge;}
    public int getUserRestPulse() {
        return userRestPulse;
    }

    private static String getTimeStamp(String dateStr) {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.GERMANY);
        String timestamp = "";

        today = today.length() < 2 ? "0" + today : today;

        try {
            Date date = format.parse(dateStr);
            SimpleDateFormat todayFormat = new SimpleDateFormat("dd", Locale.GERMANY);
            String dateToday = todayFormat.format(date);
            format = dateToday.equals(today) ? new SimpleDateFormat("HH:mm", Locale.GERMANY) : new SimpleDateFormat("dd LLL, HH:mm", Locale.GERMANY);
            timestamp = format.format(date);
        } catch (ParseException e) {
            e.printStackTrace();
        }

        return timestamp;
    }
}

