package com.breadwallet.tools.adapter;

import android.app.Activity;
import android.content.Context;
import android.graphics.Typeface;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;

import com.breadwallet.R;
import com.breadwallet.presenter.customviews.BaseTextView;
import com.breadwallet.presenter.entities.TokenItem;
import com.breadwallet.tools.util.BRConstants;
import com.breadwallet.wallet.WalletsMaster;
import com.breadwallet.wallet.abstracts.BaseWalletManager;
import com.breadwallet.wallet.wallets.ethereum.WalletTokenManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class AddTokenListAdapter extends RecyclerView.Adapter<AddTokenListAdapter.TokenItemViewHolder> {

    private Context mContext;
    private List<TokenItem> mTokens;
    private List<TokenItem> mBackupTokens;
    private static final String TAG = AddTokenListAdapter.class.getSimpleName();
    private OnTokenAddOrRemovedListener mListener;

    public AddTokenListAdapter(Context context, List<TokenItem> tokens, OnTokenAddOrRemovedListener listener) {
        mContext = context;
        mTokens = tokens;
        mListener = listener;
        mBackupTokens = mTokens;

        Collections.sort(mTokens, new Comparator<TokenItem>() {
            @Override
            public int compare(TokenItem first, TokenItem second) {
                return first.symbol.compareToIgnoreCase(second.symbol);
            }
        });
    }

    public interface OnTokenAddOrRemovedListener {

        void onTokenAdded(TokenItem token);

        void onTokenRemoved(TokenItem token);
    }


    @Override
    public void onBindViewHolder(final @NonNull AddTokenListAdapter.TokenItemViewHolder holder, final int position) {

        TokenItem item = mTokens.get(position);
        String currencyCode = item.symbol.toLowerCase();

        String iconResourceName = currencyCode;

        int iconResourceId = 0;
        if(currencyCode.equalsIgnoreCase("1st")){
            iconResourceId = mContext.getResources().getIdentifier("first", BRConstants.DRAWABLE, mContext.getPackageName());
        } else {
            iconResourceId = mContext.getResources().getIdentifier(currencyCode, BRConstants.DRAWABLE, mContext.getPackageName());
        }


        BaseWalletManager wallet = WalletsMaster.getInstance(mContext).getWalletByIso(mContext, item.symbol);
        if(null == wallet) {
            holder.mErc20Icon.setVisibility(View.GONE);
        } else if(wallet instanceof WalletTokenManager) {
            holder.mErc20Icon.setVisibility(View.VISIBLE);
        } else {
            holder.mErc20Icon.setVisibility(View.GONE);
        }

        holder.name.setText(mTokens.get(position).name);
        holder.symbol.setText(mTokens.get(position).symbol);
        try {
            holder.logo.setImageDrawable(mContext.getDrawable(iconResourceId));
        } catch (Exception e) {
            e.printStackTrace();
            Log.d(TAG, "Error finding icon for -> " + iconResourceName);
        }

        holder.addRemoveButton.setBackground(mContext.getDrawable(item.isAdded ? R.drawable.ic_star_selected : R.drawable.ic_star_normal));

        holder.addRemoveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                // Set button to "Remove"
                if (!mTokens.get(position).isAdded) {
                    mTokens.get(position).isAdded = true;
                    mListener.onTokenAdded(mTokens.get(position));
                } else {
                    // Set button back to "Add"
                    mTokens.get(position).isAdded = false;
                    mListener.onTokenRemoved(mTokens.get(position));

                }

            }
        });


    }

    @Override
    public int getItemCount() {
        return mTokens.size();
    }

    @NonNull
    @Override
    public AddTokenListAdapter.TokenItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        LayoutInflater inflater = ((Activity) mContext).getLayoutInflater();
        View convertView = inflater.inflate(R.layout.token_list_item, parent, false);

        TokenItemViewHolder holder = new TokenItemViewHolder(convertView);
        holder.setIsRecyclable(false);

        return holder;
    }

    public class TokenItemViewHolder extends RecyclerView.ViewHolder {

        private ImageView logo;
        private BaseTextView symbol;
        private BaseTextView name;
        private Button addRemoveButton;
        private View mErc20Icon;

        public TokenItemViewHolder(View view) {
            super(view);

            logo = view.findViewById(R.id.token_icon);
            symbol = view.findViewById(R.id.token_symbol);
            name = view.findViewById(R.id.token_name);
            addRemoveButton = view.findViewById(R.id.add_remove_button);
            mErc20Icon = view.findViewById(R.id.erc20_flag);
        }
    }

    public void resetFilter() {
        mTokens = mBackupTokens;
        notifyDataSetChanged();
    }

    public void filter(String query) {
        resetFilter();
        ArrayList<TokenItem> filteredList = new ArrayList<>();

        query = query.toLowerCase();

        for (TokenItem item : mTokens) {
            if (item.name.toLowerCase().contains(query) || item.symbol.toLowerCase().contains(query)) {
                filteredList.add(item);
            }
        }

        mTokens = filteredList;
        notifyDataSetChanged();

    }

}
