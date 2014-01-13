/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.paymentsystem;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Enumeration;
import java.util.Properties;

import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Request;
import org.apache.log4j.Logger;
import org.dspace.app.util.SubmissionInfo;
import org.dspace.app.util.SubmissionStepConfig;
import org.dspace.app.xmlui.aspect.submission.FlowUtils;

import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.utils.ContextUtil;
import org.dspace.app.xmlui.utils.HandleUtil;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.List;
import org.dspace.app.xmlui.wing.element.Options;
import org.dspace.app.xmlui.wing.element.Select;
import org.dspace.app.xmlui.wing.element.Text;
import org.dspace.authorize.AuthorizeException;

import org.dspace.content.DCValue;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.crosswalk.StreamIngestionCrosswalk;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.paymentsystem.*;
import org.dspace.workflow.ClaimedTask;
import org.dspace.workflow.DryadWorkflowUtils;
import org.dspace.workflow.WorkflowItem;
import org.xml.sax.SAXException;
import org.dspace.utils.DSpace;


/**
 * Shopping Cart Submision Step Transformer
 *
 * @author Mark Diggory, mdiggory at atmire.com
 * @author Fabio Bolognesi, fabio at atmire.com
 * @author Lantian Gai, lantian at atmire.com
 */
public class ShoppingCartTransformer extends AbstractDSpaceTransformer {

    private static final Logger log = Logger.getLogger(AbstractDSpaceTransformer.class);

    protected static final Message T_Header=
            message("xmlui.PaymentSystem.shoppingcart.order.header");

    protected static final Message T_Payer=
            message("xmlui.PaymentSystem.shoppingcart.order.payer");
    protected static final Message T_Price=
            message("xmlui.PaymentSystem.shoppingcart.order.price");
    protected static final Message T_Surcharge=
            message("xmlui.PaymentSystem.shoppingcart.order.surcharge");
    protected static final Message T_Total=
            message("xmlui.PaymentSystem.shoppingcart.order.total");
    protected static final Message T_noInteg=
            message("xmlui.PaymentSystem.shoppingcart.order.noIntegrateFee");
    protected static final Message T_Country=
            message("xmlui.PaymentSystem.shoppingcart.order.country");
    protected static final Message T_Voucher=
            message("xmlui.PaymentSystem.shoppingcart.order.voucher");
   protected static final Message T_Apply=
            message("xmlui.PaymentSystem.shoppingcart.order.apply");
    protected static final Message T_CartHelp=
            message("xmlui.PaymentSystem.shoppingcart.help");
    private static final String DSPACE_SUBMISSION_INFO = "dspace.submission.info";

    public void addOptions(Options options) throws SAXException, org.dspace.app.xmlui.wing.WingException,
            SQLException, IOException, AuthorizeException {

        Request request = ObjectModelHelper.getRequest(objectModel);

        PaymentSystemConfigurationManager manager = new PaymentSystemConfigurationManager();
        Enumeration s = request.getParameterNames();
        Enumeration v = request.getAttributeNames();
        SubmissionInfo submissionInfo=(SubmissionInfo)request.getAttribute("dspace.submission.info");

        Item item = null;
        try{
            if(submissionInfo==null)
            {
                //it is in workflow
                String workflowId = request.getParameter("workflowID");
		if(workflowId==null) {
		    // item is no longer in submission OR workflow, probably archived, so we don't need shopping cart info
		    return;
		}
                WorkflowItem workflowItem = WorkflowItem.find(context,Integer.parseInt(workflowId));
                item = workflowItem.getItem();
            }
            else
            {
                item = submissionInfo.getSubmissionItem().getItem();
            }
            boolean initalPage=true;
            Item dataPackage = DryadWorkflowUtils.getDataPackage(context,item);
            if(dataPackage==null){
                dataPackage=item;
            }
            DCValue[] value = dataPackage.getMetadata("prism.publicationName");
            if(value!=null&&value.length>0)
            {
                //only when the select journal we will remove the country list
                initalPage = false;
            }
            if(!initalPage){
            //DryadJournalSubmissionUtils.journalProperties.get("");
            PaymentSystemService payementSystemService = new DSpace().getSingletonService(PaymentSystemService.class);
            ShoppingCart transaction = null;
            //create new transaction or update transaction id with item
            transaction = getTransaction(item, payementSystemService);
            payementSystemService.updateTotal(context,transaction,null);

            //add the order summary form (wrapped in div.ds-option-set for proper sidebar style)

            List info = options.addList("Payment",List.TYPE_FORM,"paymentsystem");

            generateOrderFrom(info,transaction,manager,payementSystemService,request.getContextPath());

            org.dspace.app.xmlui.wing.element.Item help = options.addList("need-help").addItem();
            help.addContent(T_CartHelp);
            }
        }catch (Exception pe)
        {
            log.error("Exception: ShoppingCart:", pe);
        }
    }
    }

    private void generateVoucherForm(org.dspace.app.xmlui.wing.element.List info,PaymentSystemConfigurationManager manager,ShoppingCart shoppingCart) throws WingException,SQLException{
        Voucher voucher1 = Voucher.findById(context,shoppingCart.getVoucher());
        info.addItem("errorMessage","errorMessage").addContent("");
        info.addLabel(T_Voucher);
        org.dspace.app.xmlui.wing.element.Item voucher = info.addItem("voucher-list","voucher-list");

            Text voucherText = voucher.addText("voucher","voucher");
            voucher.addButton("apply","apply");
            if(voucher1!=null){
                voucherText.setValue(voucher1.getCode());
                info.addItem("remove-voucher","remove-voucher").addXref("#","Remove Voucher : "+voucher1.getCode());
            }
            else{
                info.addItem("remove-voucher","remove-voucher").addXref("#","Remove Voucher : ");
            }



    }

    private void generatePrice(org.dspace.app.xmlui.wing.element.List info,PaymentSystemConfigurationManager manager,ShoppingCart shoppingCart,PaymentSystemService paymentSystemService) throws WingException,SQLException{
        String waiverMessage = "";
        String symbol = PaymentSystemConfigurationManager.getCurrencySymbol(shoppingCart.getCurrency());
        switch (paymentSystemService.getWaiver(context,shoppingCart,""))
        {
	case ShoppingCart.COUNTRY_WAIVER: waiverMessage = "Data Publishing Charge has been waived due to submitter's association with " + shoppingCart.getCountry() + "."; break;
	case ShoppingCart.JOUR_WAIVER: waiverMessage = "Data Publishing Charges are covered for all submissions to " + shoppingCart.getJournal() + "."; break;
	case ShoppingCart.VOUCHER_WAIVER: waiverMessage = "Voucher code applied to Data Publishing Charge."; break;
	}
        info.addLabel(T_Price);
        if(paymentSystemService.hasDiscount(context,shoppingCart,null))
        {
            info.addItem("price","price").addContent(symbol+"0");
        }
        else
        {
            info.addItem("price","price").addContent(String.format("%s%.0f", symbol, shoppingCart.getBasicFee()));
        }
        Double noIntegrateFee =  paymentSystemService.getNoIntegrateFee(context,shoppingCart,null);

        //add the no integrate fee if it is not 0
        info.addLabel(T_noInteg);
        if(!paymentSystemService.hasDiscount(context,shoppingCart,null)&&noIntegrateFee>0&&!paymentSystemService.hasDiscount(context,shoppingCart,null))
        {
            info.addItem("no-integret","no-integret").addContent(String.format("%s%.0f", symbol, noIntegrateFee));
        }
        else
        {
            info.addItem("no-integret","no-integret").addContent(symbol+"0");
        }
        generateSurchargeFeeForm(info,manager,shoppingCart,paymentSystemService);


        //add the final total price
        info.addLabel(T_Total);
        info.addItem("total","total").addContent(String.format("%s%.0f",symbol, shoppingCart.getTotal()));
        info.addItem("waiver-info","waiver-info").addContent(waiverMessage);
    }
    private void generatePayer(Request request,org.dspace.app.xmlui.wing.element.List info,ShoppingCart shoppingCart,PaymentSystemService paymentSystemService,Item item) throws WingException,SQLException{
        info.addLabel(T_Payer);
        String payerName = paymentSystemService.getPayer(context, shoppingCart, null);
        //if(request.getRequestURI().endsWith("submit"))
        DCValue[] values= item.getMetadata("prism.publicationName");
        //WorkspaceItem submission=WorkspaceItem.find(context,item.getID());
        if(values!=null&&values.length>0)
        {
            //on the first page don't generate the payer name, wait until user choose country or journal
            info.addItem("payer","payer").addContent(payerName);
        }
        else
        {
            info.addItem("payer","payer").addContent("");
        }


    }



}
