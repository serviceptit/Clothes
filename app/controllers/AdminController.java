package controllers;

import com.clothes1.services.ClothesService1;
import com.clothes2.services.ClothesService2;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import models.*;
import play.cache.Cache;
import play.db.jpa.JPA;
import play.mvc.Before;
import play.mvc.Controller;

import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import java.lang.reflect.Type;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by Ha Thanh Tam on 16/03/2016.
 */
public class AdminController extends Controller {
    private static Gson gson = new Gson();
    private static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss.S");

    @Before
    static void checkNotAdmin() {
        try {
            int position = ((Employee) Cache.get(session.get("username"))).position;
            if (position != 2) {
                redirect("../../Employee/Login-form.html");
            }
        } catch (Exception e) {
            redirect("../../Employee/Login-form.html");
        }
    }

    public static void admin() {
        render("admin/admin.html");
    }

    public static void showListClothes(int idPublisher) {
        PublisherService publisherService = (PublisherService) JPA.em()
                .createQuery("FROM PublisherService p WHERE p.publisher.id=:id")
                .setParameter("id", idPublisher).getSingleResult();
        try {
            URL url = new URL(publisherService.urlService);
            QName qName = new QName(publisherService.targetNamespace, publisherService.name);
            Service service = Service.create(url, qName);
            if (publisherService.interfaceName.equals("ClothesService1")) {
                ClothesService1 clothesService1 = service.getPort(ClothesService1.class);
                Type listType = new TypeToken<ArrayList<Clothes>>() {
                }.getType();
                List<Clothes> clothesList = gson.fromJson(clothesService1.findAll(), listType);
                Cache.set(session.get("username") + "clothesList" + idPublisher, clothesList);
                renderArgs.put("clothesList", clothesList);
            }
            if (publisherService.interfaceName.equals("ClothesService2")) {
                ClothesService2 clothesService2 = service.getPort(ClothesService2.class);
                Type listType = new TypeToken<ArrayList<Clothes>>() {
                }.getType();
                List<Clothes> clothesList = gson.fromJson(clothesService2.findAll(), listType);
                Cache.set(session.get("username") + "clothesList" + idPublisher, clothesList);
                renderArgs.put("clothesList", clothesList);
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        render("admin/show.list.clothes.html", idPublisher);
    }

    public synchronized static void addClothesPublisher(int idPublisher, int index, float price, int sale, float voucher, String timeStart, String timeStop) {
        List<Clothes> clothesList = (List<Clothes>) Cache.get(session
                .get("username") + "clothesList" + idPublisher);
        Clothes clothes = clothesList.get(index);
        clothes.publisher = Publisher.findById(idPublisher);
        try {
            Deal deal = new Deal();
            deal.id = 0;
            if (!timeStart.equals("") && !timeStop.equals("")) {
                Date start = sdf.parse(timeStart);
                Date stop = sdf.parse(timeStop);
                deal = new Deal(sale, voucher, start, stop);
                int idDeal = 0;
                try {
                    idDeal = (Integer) JPA.em().createQuery("SELECT MAX(d.id) FROM Deal d").getSingleResult() + 1;
                } catch (NullPointerException e) {
                }
                deal.id = idDeal;
                deal.save();
            }
            clothes.price = price;
            clothes.deal = deal;
            int idClothes = 0;
            try {
                idClothes = (Integer) JPA.em().createQuery("SELECT MAX(c.id) FROM Clothes c").getSingleResult() + 1;
            } catch (NullPointerException e) {
            }
            clothes.id = idClothes;
            clothes.save();
        } catch (Exception e) {
            e.printStackTrace();
        }
        redirect("../Show-list-clothes.html?idPublisher=" + idPublisher);
    }

    public static void searchClothes(String txtSearch) {
        List<Clothes> clothesList = JPA.em().createQuery("FROM Clothes c WHERE c.title LIKE :txtSearch")
                .setParameter("txtSearch", "%" + txtSearch + "%").getResultList();
        renderArgs.put("clothesList", clothesList);
        render("admin/list.clothes.html");
    }

    public synchronized static void setDeal(String idCheck, int sale, float voucher, String timeStart, String timeStop) throws ParseException {
        String idClothes[] = idCheck.split(" ");
        int id = 0;
        Date start = sdf.parse(timeStart);
        Date stop = sdf.parse(timeStop);
        Deal deal;
        for (int i = 0; i < idClothes.length; i++) {
            id = Integer.parseInt(idClothes[i]);
            Clothes clothes = Clothes.findById(id);
            deal = clothes.deal;
            if (deal.id == 0) {
                deal = new Deal();
                int idDeal = 0;
                try {
                    idDeal = (Integer) JPA.em().createQuery("SELECT MAX(d.id) FROM Deal d").getSingleResult() + 1;
                } catch (NullPointerException e) {
                }
                deal.id = idDeal;
                deal.timeStart = start;
                deal.timeStop = stop;
                deal.voucher = voucher;
                deal.sale = sale;
            } else {
                deal.timeStart = start;
                deal.timeStop = stop;
                deal.voucher = voucher;
                deal.sale = sale;
            }
            deal.save();
            clothes.deal = deal;
            clothes.save();
        }
        redirect("../../List-clothes.html");
    }

    public static void saveClothes(int[] idCheck) throws ParseException {
        Clothes clothes = null;
        Deal deal = null;
        Date start = null;
        Date stop = null;
        for (int i = 0; i < idCheck.length; i++) {
            clothes = Clothes.findById(idCheck[i]);
            clothes.price = Float.parseFloat(params.get("price" + idCheck[i]));
            deal = clothes.deal;
            start = sdf.parse(params.get("timeStart" + idCheck[i]));
            stop = sdf.parse(params.get("timeStop" + idCheck[i]));
            if (deal.id == 0) {
                deal = new Deal();
                int idDeal = 0;
                try {
                    idDeal = (Integer) JPA.em().createQuery("SELECT MAX(d.id) FROM Deal d").getSingleResult() + 1;
                } catch (NullPointerException e) {
                }
                deal.id = idDeal;
                deal.timeStart = start;
                deal.timeStop = stop;
                deal.voucher = Float.parseFloat(params.get("voucher" + idCheck[i]));
                deal.sale = Integer.parseInt(params.get("sale" + idCheck[i]));
            } else {
                deal.timeStart = start;
                deal.timeStop = stop;
                deal.voucher = Float.parseFloat(params.get("voucher" + idCheck[i]));
                deal.sale = Integer.parseInt(params.get("sale" + idCheck[i]));
            }
            deal.save();
            clothes.deal = deal;
            clothes.save();
        }
        redirect("../List-clothes.html");
    }

    public synchronized static void deleteClothes(int[] idCheck) {
        Clothes clothes = null;
        Deal deal = null;
        for (int i = 0; i < idCheck.length; i++) {
            clothes = Clothes.findById(idCheck[i]);
            deal = clothes.deal;
            clothes.delete();
            if (deal.id != 0) {
                deal.delete();
            }
        }
        redirect("../List-clothes.html");
    }

    public static void showListPublisher() {
        List<Publisher> listPublisher = Publisher.findAll();
        renderArgs.put("listPublisher", listPublisher);
        render("admin/show.list.publisher.html");
    }

    public static void listClothes() {
        List<Clothes> clothesList = Clothes.findAll();
        renderArgs.put("clothesList", clothesList);
        render("admin/list.clothes.html");
    }

    public static void showListBill() {
        List<Bill> bills = Bill.findAll();
        render("admin/list.bill.html", bills);
    }

    public static void searchBillByStatus(String status) {
        List<Bill> bills = Bill.find("FROM Bill b WHERE b.status=:status")
                .setParameter("status", status).fetch();
        render("admin/list.bill.html", bills);
    }

    public static void viewBillById(int id) {
        Bill bill = Bill.findById(id);
        renderArgs.put("bill", bill);
        render("bill/view.bill.by.id.html");
    }

    public static void saveBill(int[] idCheck) {
        Bill bill;
        for (int i = 0; i < idCheck.length; i++) {
            bill = Bill.findById(idCheck[i]);
            bill.status = params.get("status" + idCheck[i]);
            Customer customer = Customer.findById(bill.customer.id);
            if (params.get("status" + idCheck[i]).equals("Hủy đơn hàng")) {
                customer.point += bill.usePoint;
                customer.save();
                bill.customer = customer;
                Mails.sendCancelOrder(bill);
            } else {
                if (params.get("status" + idCheck[i]).equals("Đơn hàng đã chuyển thành công")) {
                    customer.point += bill.getPointPlus();
                    customer.save();
                    bill.customer = customer;
                    Mails.sendCompletedOrder(bill);
                } else {
                    bill.customer = customer;
                    Mails.sendStatusOrder(bill);
                }
            }
            bill.save();
        }
        redirect("../Show-list-bill.html");
    }
}
