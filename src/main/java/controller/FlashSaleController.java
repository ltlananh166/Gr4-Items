package controller;

import exception.InvaildDiscountException;
import exception.InvaildEventException;
import model.FlashItem;
import model.FlashSaleEvent;
import repository.FlashSaleEventRepository;
import repository.FlashItemRepository;
import service.FlashSaleEventService;
import service.FlashSalePolicy;
import java.time.LocalDtaTime;
import java.util.List;
import java.util.stream.Collectors;

public class FlashSaleController {
    private final FlashItemRepository flashItemRepository;
    private final FlashSaleEventService eventService;

    public FlashSaleController(FlashItemRepository flashItemRepository){
        this(flashItemRepository,(FlashSaleEventService) null);
    }
    public FlashSaleController(FlashItemRepository flashItemRepository, FlashSaleEventRepository eventRepository){
        this(flashItemRepository , new FlashSaleEventService(eventRepository));
    }
    public FlashSaleController(FlashItemRepository flashItemRepository, FlashItemSaleEventService eventService){
        this.flashItemRepository = flashItemRepository;
        this.eventService  = eventService;
    }
    public List<FlashItem>
    getFlashSaleItems(int limit) {
        return 
        flashItemRepository.findAll().stream().limit(Math.max(0,limit)).collect(Collectors.toList());
    }
    public FlashItem addFlashSaleItem(FlashItem item)
    throws InvaildDiscountException {
        FlashSalePolicy.validateDiscount(item);
        if 
        (flashItemRepository.findById(item.getItemId()) != null){
            throws new 
            IllegalArgumentException("Ma Flash SALE ITEM da ton tai:" + item.getItemId());
        }
        flashItemRepository.save(item);
        return item;
    }
    public FlasjItem 
    findFlashSaleItem(String itemId){
        return
        flashItemRepository.findById(itemId);
    }
    public List<FlashItem>
    getItemsByEvent(String eventId) {
        if (eventId == null ||
        eventId.isBlank()){
            return List.of();
        }
        return 
        flashItemRepository.findAll().stream().filter(item -> eventId.equalsIgnoreCase(item.getEventId()))
        .toList();
    }
    public boolean
    updateFlashSaleItem(FlashItem item,int expectedVersion)
        throws InvaildDiscountException{
            FlashSalePolicy.validateDiscount(item);
            return
            flashItemRepository.updateItem(item,expectedVersion);
        }
    
    public boolean
    deleteFlashSaleItem(String itemId){
        return
        flashItemRepository.deleteById(itemId);
    }
    public FlashSaleEvent 
    createEvent(FlashSaleEvent event)
    throws InvaildEventException{
        return
        requireEventService().startEvent(eventId);
    }
    public FlashSaleEvent startEvetn(String eventId, LocalDtaTime now)
    throws InvaildEventException{
        return
        requireEventService().startEvent(eventId, now);
    }
    public FlashSaleEvent 
    endEvent(String eventId) throws 
    InvaildEventException {
        return 
        requireEventService().endEvent(eventId);
    }
    public List<FlashSaleEvent>
    listEvent(){
        return
        requireEventService().listEvent();
    }
    public List<FlashSaleEvent>
    listCustomerVisibleEvent(){
        return listEvent().stream()
        .filter(event -> event.getSaleStatus() ==
        SaleStatus.ACTIVE
        || event.getSaleStatus() == SaleStatus.LOCKED)
        .toList();
    }
    public FlashSaleEvent
    updateEvent(String eventId) throws
    InvaildEventException {
        return
        requireEventService().getEvent(eventId);
    }
    public FlashSaleEvent
    updateEvent(FlashSaleEvent event)
    throws InvaildEventException {
        return
        requireEventService().updateEvent(event);
    }
    public boolean deleteEvent(String eventId)
    throws InvaildEventException {
        if(!
        getItemsByEvent(eventId).isEmpty()) {
            throw new 
            InvaildEventException(
                "Hay xoa toan bo su kien FlashSale item truoc khi xoa su kien."
            );
        }
        return
        requireEventService().deleteEvent(eventId);
    }
    public FlashSaleEvent 
    lockEvent(String eventId, LocalDtaTime unlockAt)
    throws InvaildEventException {
        return
        requireEventService().lockEvent(eventId,unlockAt);
    }
    public FlashSaleEvent 
    unlockREvent(String eventId) throws 
    InvaildEventException{
        return
        requireEventService().unlockEvent(eventId);
    }
    private FlashSaleEventService requireEventService(){
        if(eventService == null){
            throw new
            IllegelStateException("FlashSaleEventService chua duoc cau hinh");
        }
        return eventService;
    }
    }








}