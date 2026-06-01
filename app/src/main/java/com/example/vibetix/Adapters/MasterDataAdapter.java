package com.example.vibetix.Adapters;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import com.example.vibetix.Fragments.Admin.CategoryManagerFragment;
import com.example.vibetix.Fragments.Admin.VenueManagerFragment;

public class MasterDataAdapter extends FragmentStateAdapter {

    public MasterDataAdapter(@NonNull Fragment fragment) {
        super(fragment);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        if (position == 0) {
            return new CategoryManagerFragment();
        } else {
            return new VenueManagerFragment();
        }
    }

    @Override
    public int getItemCount() {
        return 2;
    }
}
