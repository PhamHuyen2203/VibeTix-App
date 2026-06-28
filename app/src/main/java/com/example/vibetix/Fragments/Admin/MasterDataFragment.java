package com.example.vibetix.Fragments.Admin;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.example.vibetix.R;
import com.example.vibetix.Models.Category;
import com.example.vibetix.Models.Destination;
import com.example.vibetix.Repositories.CategoryRepository;
import com.example.vibetix.Repositories.DestinationRepository;
import com.example.vibetix.Adapters.MasterDataAdapter;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayoutMediator;

public class MasterDataFragment extends Fragment {

    private ViewPager2 viewPager;
    private TabLayout tabLayout;
    private FloatingActionButton fab;
    private CategoryRepository categoryRepository;
    private DestinationRepository destinationRepository;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_master_data, container, false);

        categoryRepository = new CategoryRepository();
        destinationRepository = new DestinationRepository();

        viewPager = view.findViewById(R.id.view_pager_master);
        tabLayout = view.findViewById(R.id.tab_layout_master);
        fab = view.findViewById(R.id.fab_add_master);

        setupViewPager();
        setupFab();

        return view;
    }

    private void setupViewPager() {
        if (viewPager != null && tabLayout != null) {
            MasterDataAdapter adapter = new MasterDataAdapter(this);
            viewPager.setAdapter(adapter);
            new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
                tab.setText(position == 0 ? getString(R.string.tab_categories) : getString(R.string.tab_venues));
            }).attach();
        }
    }

    private void setupFab() {
        if (fab != null) {
            fab.setOnClickListener(v -> {
                if (viewPager != null) {
                    int currentTab = viewPager.getCurrentItem();
                    if (currentTab == 0) {
                        showAddCategoryDialog();
                    } else {
                        showAddVenueDialog();
                    }
                }
            });
        }
    }

    private void showAddCategoryDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_category, null);
        EditText etName = dialogView.findViewById(R.id.et_category_name);
        EditText etSlug = dialogView.findViewById(R.id.et_category_slug);

        AlertDialog dialog = new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.add_category_title)
                .setView(dialogView)
                .setPositiveButton(R.string.btn_save, null)
                .setNegativeButton(R.string.btn_cancel, (d, w) -> d.dismiss())
                .create();

        dialog.show();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            String slug = etSlug.getText().toString().trim();

            if (name.isEmpty() || slug.isEmpty()) {
                Toast.makeText(getContext(), getString(R.string.error_empty_fields), Toast.LENGTH_SHORT).show();
                return;
            }

            Category category = new Category();
            category.setName(name);
            category.setSlug(slug);

            categoryRepository.addCategory(category).addOnSuccessListener(doc -> {
                Toast.makeText(getContext(), "Đã thêm danh mục: " + name, Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            }).addOnFailureListener(e -> Toast.makeText(getContext(), "Lỗi khi lưu", Toast.LENGTH_SHORT).show());
        });
    }

    private void showAddVenueDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_venue, null);
        EditText etName = dialogView.findViewById(R.id.et_venue_name);
        EditText etAddress = dialogView.findViewById(R.id.et_venue_address);
        EditText etCity = dialogView.findViewById(R.id.et_venue_city);
        EditText etCapacity = dialogView.findViewById(R.id.et_venue_capacity);

        AlertDialog dialog = new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.add_venue_title)
                .setView(dialogView)
                .setPositiveButton(R.string.btn_save, null)
                .setNegativeButton(R.string.btn_cancel, (d, w) -> d.dismiss())
                .create();

        dialog.show();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            String address = etAddress.getText().toString().trim();
            String city = etCity.getText().toString().trim();
            String capacityStr = etCapacity.getText().toString().trim();

            if (name.isEmpty() || address.isEmpty() || city.isEmpty() || capacityStr.isEmpty()) {
                Toast.makeText(getContext(), getString(R.string.error_empty_fields), Toast.LENGTH_SHORT).show();
                return;
            }

            Destination dest = new Destination();
            dest.setName(name);
            dest.setAddress(address);
            dest.setCity(city);
            try {
                dest.setCapacity(Integer.parseInt(capacityStr));
                destinationRepository.addDestination(dest).addOnSuccessListener(doc -> {
                    Toast.makeText(getContext(), "Đã thêm địa điểm: " + name, Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                }).addOnFailureListener(e -> Toast.makeText(getContext(), "Lỗi khi lưu", Toast.LENGTH_SHORT).show());
            } catch (NumberFormatException e) {
                Toast.makeText(getContext(), getString(R.string.error_invalid_number), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
