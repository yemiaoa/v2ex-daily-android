package com.yugy.v2ex.daily.widget;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.yugy.v2ex.daily.R;
import com.yugy.v2ex.daily.activity.NodeActivity;
import com.yugy.v2ex.daily.activity.UserActivity;
import com.yugy.v2ex.daily.model.MemberModel;
import com.yugy.v2ex.daily.model.TopicModel;
import com.yugy.v2ex.daily.network.RequestManager;

/**
 * Created by yugy on 14-2-23.
 */
public class TopicView extends RelativeLayout implements View.OnClickListener{
    public TopicView(Context context) {
        super(context);
        init();
    }

    public TopicView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public TopicView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private TextView mTitle;
    private TextView mContent;
    private SelectorImageView mHead;
    private TextView mName;
    private RelativeTimeTextView mTime;
    private TextView mReplies;
    private TextView mNode;

    private int mNodeId;
    private MemberModel mMember;

    private void init(){
        inflate(getContext(), R.layout.view_topic, this);
        mTitle = (TextView) findViewById(R.id.txt_view_topic_title);
        mContent = (TextView) findViewById(R.id.txt_view_topic_content);
        mHead = (SelectorImageView) findViewById(R.id.img_view_topic_head);
        mName = (TextView) findViewById(R.id.txt_view_topic_name);
        mTime = (RelativeTimeTextView) findViewById(R.id.txt_view_topic_time);
        mReplies = (TextView) findViewById(R.id.txt_view_topic_replies);
        mNode = (TextView) findViewById(R.id.txt_view_topic_node);

        mHead.setOnClickListener(this);
        mNode.setOnClickListener(this);
    }

    public void parse(TopicModel model){
        mTitle.setText(model.title);
        mContent.setText(Html.fromHtml(model.contentRendered));

        mName.setText(model.member.username);
        mTime.setReferenceTime(model.created * 1000);
        mReplies.setText(model.replies + " 个回复");
        mNode.setText(model.node.name);

        mMember = model.member;
        mNodeId = model.node.id;

        RequestManager.getInstance().displayImage(model.member.avatarLarge, mHead);
    }

    public void setViewDetail(){
        mContent.setMaxLines(Integer.MAX_VALUE);
        mContent.setTextSize(14);
    }

    @Override
    public void onClick(View v) {
        Intent intent;
        Bundle argument;
        switch (v.getId()){
            case R.id.img_view_topic_head:
                intent = new Intent(getContext(), UserActivity.class);
                argument = new Bundle();
                argument.putParcelable("model", mMember);
                intent.putExtra("argument", argument);
                getContext().startActivity(intent);
                break;
            case R.id.txt_view_topic_node:
                intent = new Intent(getContext(), NodeActivity.class);
                argument = new Bundle();
                argument.putInt("node_id", mNodeId);
                intent.putExtra("argument", argument);
                getContext().startActivity(intent);
                break;
        }
    }
}
