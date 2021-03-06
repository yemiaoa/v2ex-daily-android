package com.yugy.v2ex.daily.fragment;

import android.app.ActionBar;
import android.app.Fragment;
import android.content.Intent;
import android.graphics.RectF;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.yugy.v2ex.daily.R;
import com.yugy.v2ex.daily.activity.TopicActivity;
import com.yugy.v2ex.daily.adapter.LoadingAdapter;
import com.yugy.v2ex.daily.adapter.TopicAdapter;
import com.yugy.v2ex.daily.model.MemberModel;
import com.yugy.v2ex.daily.model.TopicModel;
import com.yugy.v2ex.daily.network.RequestManager;
import com.yugy.v2ex.daily.sdk.V2EX;
import com.yugy.v2ex.daily.utils.DebugUtils;
import com.yugy.v2ex.daily.utils.ScreenUtils;
import com.yugy.v2ex.daily.widget.AlphaForegroundColorSpan;
import com.yugy.v2ex.daily.widget.AppMsg;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.EOFException;
import java.util.ArrayList;

import uk.co.senab.actionbarpulltorefresh.library.ActionBarPullToRefresh;
import uk.co.senab.actionbarpulltorefresh.library.PullToRefreshLayout;
import uk.co.senab.actionbarpulltorefresh.library.listeners.OnRefreshListener;

/**
 * Created by yugy on 14-2-25.
 */
public class UserFragment extends Fragment implements OnRefreshListener, AdapterView.OnItemClickListener{

    private int mActionBarTitleColor;
    private int mHeaderHeight;
    private int mMinHeaderTranslation;
    private View mHeader;
    private View mPlaceHolderView;
    private AccelerateDecelerateInterpolator mSmoothInterpolator;

    private RectF mRect1 = new RectF();
    private RectF mRect2 = new RectF();

    private AlphaForegroundColorSpan mAlphaForegroundColorSpan;
    private SpannableString mSpannableString;

    private PullToRefreshLayout mPullToRefreshLayout;
    private ListView mListView;
    private ImageView mHeaderLogo;
    private TextView mName;
    private TextView mDescription;

    private MemberModel mMemberModel;
    private ArrayList<TopicModel> mModels;

    private boolean mDataLoaded = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mSmoothInterpolator = new AccelerateDecelerateInterpolator();
        mHeaderHeight = ScreenUtils.dp(getActivity(), 250);
        mMinHeaderTranslation = -mHeaderHeight + ScreenUtils.getActionBarHeight(getActivity());

        mActionBarTitleColor = getResources().getColor(android.R.color.white);

        mAlphaForegroundColorSpan = new AlphaForegroundColorSpan(mActionBarTitleColor);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mPullToRefreshLayout = (PullToRefreshLayout) inflater.inflate(R.layout.fragment_user, container, false);
        mListView = (ListView) mPullToRefreshLayout.findViewById(R.id.list_fragment_user);
        mHeader = mPullToRefreshLayout.findViewById(R.id.header_fragment_user);
        mHeaderLogo = (ImageView) mPullToRefreshLayout.findViewById(R.id.header_logo_fragment_user);
        mName = (TextView) mPullToRefreshLayout.findViewById(R.id.txt_fragment_user_name);
        mDescription = (TextView) mPullToRefreshLayout.findViewById(R.id.txt_fragment_user_description);
        mPlaceHolderView = getActivity().getLayoutInflater().inflate(R.layout.view_header_placeholder, mListView, false);
        mListView.addHeaderView(mPlaceHolderView);
        mListView.setOnItemClickListener(this);
        return mPullToRefreshLayout;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        ActionBarPullToRefresh.from(getActivity())
                .listener(this)
                .allChildrenArePullable()
                .setup(mPullToRefreshLayout);

        mMemberModel = getArguments().getParcelable("model");
        mSpannableString = new SpannableString(mMemberModel.username);
        mName.setText(mMemberModel.username);
        mDescription.setText("V2EX 第 " + mMemberModel.id + " 号会员");
        RequestManager.getInstance().displayImage(mMemberModel.avatarLarge, mHeaderLogo);

        setupActionBar();
        setupListView();
    }

    private void setupListView() {
        mListView.setAdapter(new LoadingAdapter(getActivity()));
        mListView.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                int scrollY = getScrollY();
                mHeader.setTranslationY(Math.max(-scrollY, mMinHeaderTranslation));
                float ratio = clamp(mHeader.getTranslationY() / mMinHeaderTranslation, 0.0f, 1.0f);
                interpolate(mHeaderLogo, getActionBarIconView(), mSmoothInterpolator.getInterpolation(ratio));
                setTitleAlpha(clamp(5.0F * ratio - 4.0F, 0.0F, 1.0F));
            }
        });
        getData(false);
    }

    private void getData(boolean forceRefresh){
        V2EX.showTopicByUsername(getActivity(), forceRefresh, mMemberModel.username,
                new Response.Listener<JSONArray>() {

                    @Override
                    public void onResponse(JSONArray jsonArray) {
                        DebugUtils.log(jsonArray);
                        try {
                            mModels = getModels(jsonArray);
                            mListView.setAdapter(new TopicAdapter(getActivity(), mModels));
                            mDataLoaded = true;
                        } catch (JSONException e) {
                            AppMsg.makeText(getActivity(), "Json decode error", AppMsg.STYLE_ALERT).show();
                            e.printStackTrace();
                        }
                        mPullToRefreshLayout.setRefreshComplete();
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError volleyError) {
                        if (volleyError.getCause() instanceof EOFException) {
                            AppMsg.makeText(getActivity(), "Network error", AppMsg.STYLE_ALERT).show();
                        }
                        mPullToRefreshLayout.setRefreshComplete();
                        volleyError.printStackTrace();
                    }
                }
        );
    }

    private ArrayList<TopicModel> getModels(JSONArray response) throws JSONException {
        ArrayList<TopicModel> models = new ArrayList<TopicModel>();
        for(int i = 0; i < response.length(); i++){
            TopicModel model = new TopicModel();
            model.parse(response.getJSONObject(i));
            models.add(model);
        }
        return models;
    }

    private void setTitleAlpha(float alpha) {
        mAlphaForegroundColorSpan.setAlpha(alpha);
        mSpannableString.setSpan(mAlphaForegroundColorSpan, 0, mSpannableString.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        getActivity().getActionBar().setTitle(mSpannableString);
        mName.setAlpha(1.0f - alpha);
        mDescription.setAlpha(1.0f - alpha);
    }

    public static float clamp(float value, float max, float min) {
        return Math.max(Math.min(value, min), max);
    }

    private void interpolate(View view1, View view2, float interpolation) {
        getOnScreenRect(mRect1, view1);
        getOnScreenRect(mRect2, view2);

        float scaleX = 1.0F + interpolation * (mRect2.width() / mRect1.width() - 1.0F);
        float scaleY = 1.0F + interpolation * (mRect2.height() / mRect1.height() - 1.0F);
        float translationX = 0.5F * (interpolation * (mRect2.left + mRect2.right - mRect1.left - mRect1.right));
        float translationY = 0.5F * (interpolation * (mRect2.top + mRect2.bottom - mRect1.top - mRect1.bottom));

        view1.setTranslationX(translationX);
        view1.setTranslationY(translationY - mHeader.getTranslationY());
        view1.setScaleX(scaleX);
        view1.setScaleY(scaleY);
    }

    private RectF getOnScreenRect(RectF rect, View view) {
        rect.set(view.getLeft(), view.getTop(), view.getRight(), view.getBottom());
        return rect;
    }

    public int getScrollY() {
        View c = mListView.getChildAt(0);
        if (c == null) {
            return 0;
        }

        int firstVisiblePosition = mListView.getFirstVisiblePosition();
        int top = c.getTop();

        int headerHeight = 0;
        if (firstVisiblePosition >= 1) {
            headerHeight = mPlaceHolderView.getHeight();
        }

        return -top + firstVisiblePosition * c.getHeight() + headerHeight;
    }

    private void setupActionBar() {
        ActionBar actionBar = getActivity().getActionBar();
        actionBar.setIcon(R.drawable.ic_transparent);

        //getActionBarTitleView().setAlpha(0f);
    }

    private ImageView getActionBarIconView() {
        return (ImageView) getActivity().findViewById(android.R.id.home);
    }

    @Override
    public void onRefreshStarted(View view) {
        getData(true);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if(mDataLoaded){
            Intent intent = new Intent(getActivity(), TopicActivity.class);
            Bundle argument = new Bundle();
            argument.putParcelable("model", mModels.get(position));
            intent.putExtra("argument", argument);
            startActivity(intent);
        }
    }
}
