package com.example.budgettracker.service;


import com.example.budgettracker.dto.Budgetcoin.ItemResponse;
import com.example.budgettracker.model.Item;
import com.example.budgettracker.repository.ItemRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ItemService {

    private final ItemRepository itemRepository;

    public ItemService(ItemRepository itemRepository) {
        this.itemRepository = itemRepository;
    }

    public List<ItemResponse> getAllItems() {
        List<Item> items = itemRepository.findAll();
        return items.stream()
                .map(i -> new ItemResponse(
                        i.getItemId(),
                        i.getItemName(),
                        i.getPrice(),
                        i.getStockQty(),
                        i.getDescription(),
                        i.getEmoji()
                ))
                .collect(Collectors.toList());
    }
}
