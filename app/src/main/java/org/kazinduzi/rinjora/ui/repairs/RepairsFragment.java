package org.kazinduzi.rinjora.ui.repairs;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.tabs.TabLayout;

import org.kazinduzi.rinjora.R;
import org.kazinduzi.rinjora.databinding.FragmentRepairsBinding;
import org.kazinduzi.rinjora.ui.workshop.WorkshopTasksFragment;

public class RepairsFragment extends Fragment {

    private static final String TAB_REQUESTS = "tab_requests";
    private static final String TAB_TASKS = "tab_tasks";

    private FragmentRepairsBinding binding;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentRepairsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("My Requests"));
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("My Tasks"));

        FragmentManager fm = getChildFragmentManager();
        if (fm.findFragmentByTag(TAB_REQUESTS) == null && fm.findFragmentByTag(TAB_TASKS) == null) {
            fm.beginTransaction()
                    .replace(R.id.tab_container, new RepairRequestsFragment(), TAB_REQUESTS)
                    .commit();
        } else if (fm.findFragmentByTag(TAB_TASKS) != null) {
            binding.tabLayout.getTabAt(1).select();
        }

        binding.tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                Fragment f = tab.getPosition() == 0 ? new RepairRequestsFragment() : new WorkshopTasksFragment();
                String tag = tab.getPosition() == 0 ? TAB_REQUESTS : TAB_TASKS;
                getChildFragmentManager().beginTransaction()
                        .replace(R.id.tab_container, f, tag)
                        .commit();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
