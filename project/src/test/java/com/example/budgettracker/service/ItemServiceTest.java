package com.example.budgettracker.service;

import com.example.budgettracker.dto.Budgetcoin.ItemResponse;
import com.example.budgettracker.model.Item;
import com.example.budgettracker.repository.ItemRepository;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(org.mockito.junit.MockitoJUnitRunner.class)
public class ItemServiceTest {

    private static final Long ITEM_ID_1 = 1L;
    private static final Long ITEM_ID_2 = 2L;
    private static final String ITEM_NAME_1 = "Premium Theme";
    private static final String ITEM_NAME_2 = "Extra Storage";
    private static final Long PRICE_1 = 500L;
    private static final Long PRICE_2 = 1000L;
    private static final Long STOCK_QTY_1 = 10L;
    private static final Long STOCK_QTY_2 = 5L;
    private static final String DESCRIPTION_1 = "Unlock premium themes";
    private static final String DESCRIPTION_2 = "Get 10GB extra storage";
    private static final String EMOJI_1 = "🎨";
    private static final String EMOJI_2 = "💾";

    @Mock
    private ItemRepository itemRepository;

    @InjectMocks
    private ItemService itemService;

    private Item item1;
    private Item item2;

    @Before
    public void setUp() {
        item1 = new Item();
        item1.setItemId(ITEM_ID_1);
        item1.setItemName(ITEM_NAME_1);
        item1.setPrice(PRICE_1);
        item1.setStockQty(STOCK_QTY_1);
        item1.setDescription(DESCRIPTION_1);
        item1.setEmoji(EMOJI_1);

        item2 = new Item();
        item2.setItemId(ITEM_ID_2);
        item2.setItemName(ITEM_NAME_2);
        item2.setPrice(PRICE_2);
        item2.setStockQty(STOCK_QTY_2);
        item2.setDescription(DESCRIPTION_2);
        item2.setEmoji(EMOJI_2);
    }

    // ==================== getAllItems Tests ====================

    @Test
    public void getAllItems_returnsAllItems_whenItemsExist() {
        when(itemRepository.findAll()).thenReturn(Arrays.asList(item1, item2));

        List<ItemResponse> result = itemService.getAllItems();

        assertNotNull(result);
        assertThat(result.size(), is(2));

        ItemResponse response1 = result.get(0);
        assertThat(response1.getItemId(), is(ITEM_ID_1));
        assertThat(response1.getItemName(), is(ITEM_NAME_1));
        assertThat(response1.getPrice(), is(PRICE_1));
        assertThat(response1.getStockQty(), is(STOCK_QTY_1));
        assertThat(response1.getDescription(), is(DESCRIPTION_1));
        assertThat(response1.getEmoji(), is(EMOJI_1));

        ItemResponse response2 = result.get(1);
        assertThat(response2.getItemId(), is(ITEM_ID_2));
        assertThat(response2.getItemName(), is(ITEM_NAME_2));
        assertThat(response2.getPrice(), is(PRICE_2));
        assertThat(response2.getStockQty(), is(STOCK_QTY_2));
        assertThat(response2.getDescription(), is(DESCRIPTION_2));
        assertThat(response2.getEmoji(), is(EMOJI_2));

        verify(itemRepository).findAll();
    }

    @Test
    public void getAllItems_returnsEmptyList_whenNoItems() {
        when(itemRepository.findAll()).thenReturn(Collections.emptyList());

        List<ItemResponse> result = itemService.getAllItems();

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(itemRepository).findAll();
    }

    @Test
    public void getAllItems_returnsSingleItem_whenOnlyOneItemExists() {
        when(itemRepository.findAll()).thenReturn(Arrays.asList(item1));

        List<ItemResponse> result = itemService.getAllItems();

        assertNotNull(result);
        assertThat(result.size(), is(1));
        assertThat(result.get(0).getItemId(), is(ITEM_ID_1));
        verify(itemRepository).findAll();
    }

    @Test
    public void getAllItems_handlesNullFields_gracefully() {
        Item itemWithNulls = new Item();
        itemWithNulls.setItemId(ITEM_ID_1);
        itemWithNulls.setItemName(null);
        itemWithNulls.setPrice(null);
        itemWithNulls.setStockQty(null);
        itemWithNulls.setDescription(null);
        itemWithNulls.setEmoji(null);

        when(itemRepository.findAll()).thenReturn(Arrays.asList(itemWithNulls));

        List<ItemResponse> result = itemService.getAllItems();

        assertNotNull(result);
        assertThat(result.size(), is(1));
        ItemResponse response = result.get(0);
        assertThat(response.getItemId(), is(ITEM_ID_1));
        assertNull(response.getItemName());
        assertNull(response.getPrice());
        assertNull(response.getStockQty());
        assertNull(response.getDescription());
        assertNull(response.getEmoji());
    }

    @Test
    public void getAllItems_mapsAllFieldsCorrectly() {
        when(itemRepository.findAll()).thenReturn(Arrays.asList(item1));

        List<ItemResponse> result = itemService.getAllItems();

        ItemResponse response = result.get(0);
        assertThat(response.getItemId(), is(item1.getItemId()));
        assertThat(response.getItemName(), is(item1.getItemName()));
        assertThat(response.getPrice(), is(item1.getPrice()));
        assertThat(response.getStockQty(), is(item1.getStockQty()));
        assertThat(response.getDescription(), is(item1.getDescription()));
        assertThat(response.getEmoji(), is(item1.getEmoji()));
    }

    @Test
    public void getAllItems_preservesOrder_ofRepositoryResults() {
        when(itemRepository.findAll()).thenReturn(Arrays.asList(item1, item2));

        List<ItemResponse> result = itemService.getAllItems();

        assertThat(result.get(0).getItemId(), is(ITEM_ID_1));
        assertThat(result.get(1).getItemId(), is(ITEM_ID_2));
    }

    @Test
    public void getAllItems_handlesLargeList() {
        Item item3 = new Item();
        item3.setItemId(3L);
        item3.setItemName("Item 3");

        Item item4 = new Item();
        item4.setItemId(4L);
        item4.setItemName("Item 4");

        Item item5 = new Item();
        item5.setItemId(5L);
        item5.setItemName("Item 5");

        when(itemRepository.findAll()).thenReturn(Arrays.asList(item1, item2, item3, item4, item5));

        List<ItemResponse> result = itemService.getAllItems();

        assertThat(result.size(), is(5));
        assertThat(result.get(0).getItemId(), is(ITEM_ID_1));
        assertThat(result.get(1).getItemId(), is(ITEM_ID_2));
        assertThat(result.get(2).getItemId(), is(3L));
        assertThat(result.get(3).getItemId(), is(4L));
        assertThat(result.get(4).getItemId(), is(5L));
    }

    @Test
    public void getAllItems_handlesZeroStockQty() {
        item1.setStockQty(0L);
        when(itemRepository.findAll()).thenReturn(Arrays.asList(item1));

        List<ItemResponse> result = itemService.getAllItems();

        assertThat(result.get(0).getStockQty(), is(0L));
    }

    @Test
    public void getAllItems_handlesZeroPrice() {
        item1.setPrice(0L);
        when(itemRepository.findAll()).thenReturn(Arrays.asList(item1));

        List<ItemResponse> result = itemService.getAllItems();

        assertThat(result.get(0).getPrice(), is(0L));
    }

    @Test
    public void getAllItems_handlesEmptyStrings() {
        item1.setItemName("");
        item1.setDescription("");
        item1.setEmoji("");
        when(itemRepository.findAll()).thenReturn(Arrays.asList(item1));

        List<ItemResponse> result = itemService.getAllItems();

        assertThat(result.get(0).getItemName(), is(""));
        assertThat(result.get(0).getDescription(), is(""));
        assertThat(result.get(0).getEmoji(), is(""));
    }

    @Test
    public void getAllItems_handlesVeryLongDescription() {
        String longDescription = "A".repeat(1000);
        item1.setDescription(longDescription);
        when(itemRepository.findAll()).thenReturn(Arrays.asList(item1));

        List<ItemResponse> result = itemService.getAllItems();

        assertThat(result.get(0).getDescription(), is(longDescription));
    }

    @Test
    public void getAllItems_handlesSpecialCharactersInFields() {
        item1.setItemName("Test & <Item> 'Name'");
        item1.setDescription("Description with \"quotes\" and \n newlines");
        when(itemRepository.findAll()).thenReturn(Arrays.asList(item1));

        List<ItemResponse> result = itemService.getAllItems();

        assertThat(result.get(0).getItemName(), is("Test & <Item> 'Name'"));
        assertThat(result.get(0).getDescription(), is("Description with \"quotes\" and \n newlines"));
    }

    @Test
    public void getAllItems_handlesNegativeStockQty() {
        item1.setStockQty(-5L);
        when(itemRepository.findAll()).thenReturn(Arrays.asList(item1));

        List<ItemResponse> result = itemService.getAllItems();

        assertThat(result.get(0).getStockQty(), is(-5L));
    }

    @Test
    public void getAllItems_handlesVeryLargePrice() {
        Long largePrice = 999999999L;
        item1.setPrice(largePrice);
        when(itemRepository.findAll()).thenReturn(Arrays.asList(item1));

        List<ItemResponse> result = itemService.getAllItems();

        assertThat(result.get(0).getPrice(), is(largePrice));
    }

    @Test
    public void getAllItems_callsRepositoryOnce() {
        when(itemRepository.findAll()).thenReturn(Arrays.asList(item1, item2));

        itemService.getAllItems();

        verify(itemRepository, times(1)).findAll();
    }
}
