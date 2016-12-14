package com.stc.meetme.ui;

import android.content.Context;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.stc.meetme.R;
import com.stc.meetme.model.Subscription;

import java.util.ArrayList;

import de.hdodenhof.circleimageview.CircleImageView;

import static com.stc.meetme.Constants.LIST_TYPE_MY_OBSERVERS;

/**
 * Created by artem on 11/18/16.
 */


public class BaseAdapter extends RecyclerView.Adapter<BaseAdapter.BaseViewHolder> {
	private final OnListInteractionListener listener;
	Context	context;
	ArrayList<Subscription> list;
	public int listType;


	public BaseAdapter(int type, OnListInteractionListener listener, Context context) {
		this.context=context;
		this.listener=listener;
		this.list = new ArrayList<Subscription> ();
		this.listType = type;
	}

	@Override
	public BaseViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
		LinearLayout view = (LinearLayout) LayoutInflater.from(parent.getContext()).inflate(
				R.layout.item_user, parent, false);

		return new BaseViewHolder(view);
	}

	@Override
	public void onBindViewHolder(BaseViewHolder holder, final int position) {
		final Subscription subscription = list.get(position);

		holder.userName.setText(
				listType==LIST_TYPE_MY_OBSERVERS ?
						subscription.getObserverUserId() :
						subscription.getTargetUserId()
		);
		holder.userPhoto.setImageDrawable(ContextCompat.getDrawable(context,
				subscription.isActive() ?
						R.drawable.ic_active :
						R.drawable.ic_not_active));
		holder.root.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				listener.onListInteraction(list.get(position));
			}
		});
	}

	@Override
	public int getItemCount() {
		return list.size();
	}
	public void addItem(Subscription subscription){
		list.add(subscription);
		notifyItemInserted(list.indexOf(subscription));
	}


	public void removeItem(Subscription subscription) {
		//list.remove(subscription);
		notifyItemRemoved(list.indexOf(subscription));
	}

	public void updateItem(Subscription subscription) {
		notifyItemChanged(list.indexOf(subscription),subscription);
	}


	public static class BaseViewHolder extends RecyclerView.ViewHolder {
		public TextView userName;
		public CircleImageView userPhoto;
		public View root;

		public BaseViewHolder(View v) {
			super(v);
			root=v;
			userName = (TextView) v.findViewById(R.id.userName);
			userPhoto = (CircleImageView) v.findViewById(R.id.userImage);
		}
	}

}