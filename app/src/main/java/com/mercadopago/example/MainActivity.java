package com.mercadopago.example;

import android.app.Activity;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import com.mercadopago.core.MercadoPago;
import com.mercadopago.model.PaymentMethod;
import com.mercadopago.util.JsonUtil;
import com.mercadopago.util.LayoutUtil;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import android.content.Intent;
import android.view.View;
import android.widget.Toast;

import com.mercadopago.model.ApiException;
import com.mercadopago.model.Discount;
import com.mercadopago.model.Item;
import com.mercadopago.model.MerchantPayment;
import com.mercadopago.model.Payment;
import com.mercadopago.adapters.ErrorHandlingCallAdapter;
import com.mercadopago.core.MerchantServer;

import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    /*
    Estas son las public_key. Con este parametro, cada servicio de MercadoPago
    sabe a qué país pertenece el merchant para poder retornar los medios de pago disponibles
    para cada país, los tipos de monedas, etc.
     */

    // * Merchant public key
    public static final String DUMMY_MERCHANT_PUBLIC_KEY = "444a9ef5-8a6b-429f-abdf-587639155d88";
    // DUMMY_MERCHANT_PUBLIC_KEY_AR = "444a9ef5-8a6b-429f-abdf-587639155d88";
    // DUMMY_MERCHANT_PUBLIC_KEY_BR = "APP_USR-f163b2d7-7462-4e7b-9bd5-9eae4a7f99c3";
    // DUMMY_MERCHANT_PUBLIC_KEY_MX = "6c0d81bc-99c1-4de8-9976-c8d1d62cd4f2";
    // DUMMY_MERCHANT_PUBLIC_KEY_VZ = "2b66598b-8b0f-4588-bd2f-c80ca21c6d18";
    // DUMMY_MERCHANT_PUBLIC_KEY_CO = "aa371283-ad00-4d5d-af5d-ed9f58e139f1";

    /*
    * Estos parametros indican los endpoints que ustedes deben ingresar para devolver,
    * desde su servidor, las tarjetas guardadas de sus usuarios y además,
    * recibir los datos del pago para efectuarlo de forma segura con su access_token.
    * */

    // * Merchant server vars
    public static final String DUMMY_MERCHANT_BASE_URL = "https://www.mercadopago.com";
    public static final String DUMMY_MERCHANT_GET_CUSTOMER_URI = "/checkout/examples/getCustomer";
    public static final String DUMMY_MERCHANT_CREATE_PAYMENT_URI = "/checkout/examples/doPayment";

    /*
    * Si sus usuarios están logueados en su aplicación y tienen un token para identificarlos,
    * este parametro viajará cada vez que se haga un API Call con MerchantServer, utilizando los
    * parametros del paso anterior.
    * */

    // * Merchant access token
    public static final String DUMMY_MERCHANT_ACCESS_TOKEN = "mla-cards-data";
    // DUMMY_MERCHANT_ACCESS_TOKEN_AR = "mla-cards-data";
    // DUMMY_MERCHANT_ACCESS_TOKEN_BR = "mlb-cards-data";
    // DUMMY_MERCHANT_ACCESS_TOKEN_MX = "mlm-cards-data";
    // DUMMY_MERCHANT_ACCESS_TOKEN_VZ = "mlv-cards-data";
    // DUMMY_MERCHANT_ACCESS_TOKEN_VZ = "mco-cards-data";
    // DUMMY_MERCHANT_ACCESS_TOKEN_NO_CCV = "mla-cards-data-tarshop";

    /*
    * Estos son los datos del item que se va a pagar.
    * Forma parte de lo que llamamos MerchantPayment, el objeto que van a
    * estar recibiendo en sus servidores a la hora de pagar.
    * */

    // * Payment item
    public static final String DUMMY_ITEM_ID = "id1";
    public static final Integer DUMMY_ITEM_QUANTITY = 1;
    public static final BigDecimal DUMMY_ITEM_UNIT_PRICE = new BigDecimal("100");

    /*
    * Lista de los tipos de pago soportados.
    * */

    protected List<String> mSupportedPaymentTypes = new ArrayList<String>(){{
        add("credit_card");
        add("debit_card");
        add("prepaid_card");
        add("ticket");
        add("atm");
    }};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    /*
    * Cada vez que se llama a un elemento de UI de MercadoPago,
    * como un flujo (Vault) o una pantalla (Activity), se tiene que escuchar
    * los resultados que arroje ese flujo o pantalla al finalizar sus acciones.
    * */

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        /*
        * En este caso lo que vamos a estar esperando es un resultado para un
        * Vault, el famoso Flavor 2.
        * */
        if (requestCode == MercadoPago.VAULT_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {

                /* Tomamos el medio de pago seleccionado por el usuario
                  ATENCION: En este punto si seleccionó un medio de pago OFF
                 (Pago Facil, por ejemplo), los valores que vamos a estar
                 leyendo a continuación serán nulos.
                 */
                PaymentMethod paymentMethod = JsonUtil.getInstance().fromJson(data.getStringExtra("paymentMethod"), PaymentMethod.class);

                // Tomamos el banco que se eligió en el flujo
                Long issuerId = (data.getStringExtra("issuerId") != null)
                        ? Long.parseLong(data.getStringExtra("issuerId")) : null;

                // Tomamos las cuotas que seleccionó el usuario
                Integer installments = (data.getStringExtra("installments") != null)
                        ? Integer.parseInt(data.getStringExtra("installments")) : null;

                // Tomamos el token de la tarjeta seleccionada (sea nueva o guardada)
                String token = data.getStringExtra("token");

                /* Ahora estamos en condiciones de enviar
                 el pago a los servidores de Clinc para que con la clave privada
                 de la cuenta, posteen a MercadoPago y retornen la MISMA respuesta
                 */
                createPayment(this, token,
                        installments, issuerId,
                        paymentMethod, null);

            } else {

                if ((data != null) && (data.getStringExtra("apiException") != null)) {
                    Toast.makeText(getApplicationContext(), data.getStringExtra("apiException"), Toast.LENGTH_LONG).show();
                }
            }
        } else if (requestCode == MercadoPago.CONGRATS_REQUEST_CODE) {

            LayoutUtil.showRegularLayout(this);
        }
    }

    public void submitForm(View view) {

        /*
        * Se le dice a MercadoPago quien soy, a donde tiene que ir a buscar
        * las tarjetas guardadas de mis usuarios, quién es el usuario que
        * va a interactuar con el flujo, qué tipos de pago soporto y
        * cuanto quiero cobrar.
        * */
        new MercadoPago.StartActivityBuilder()
                .setActivity(this)
                .setPublicKey(DUMMY_MERCHANT_PUBLIC_KEY)
                .setMerchantBaseUrl(DUMMY_MERCHANT_BASE_URL)
                .setMerchantGetCustomerUri(DUMMY_MERCHANT_GET_CUSTOMER_URI)
                .setMerchantAccessToken(DUMMY_MERCHANT_ACCESS_TOKEN)
                .setAmount(DUMMY_ITEM_UNIT_PRICE)
                .setSupportedPaymentTypes(mSupportedPaymentTypes)
                .setShowBankDeals(true) // MUESTRA LAS PROMOCIONES PARA QUE EL USUARIO ELIJA SU MEJOR MEDIO DE PAGO.
                .startVaultActivity();
    }

    public void createPayment(final Activity activity, String token, Integer installments, Long cardIssuerId, final PaymentMethod paymentMethod, Discount discount) {

        if (paymentMethod != null) {

            LayoutUtil.showProgressLayout(activity);

            // Creamos el Item que vamos a estar cobrando
            Item item = new Item(DUMMY_ITEM_ID, DUMMY_ITEM_QUANTITY,
                    DUMMY_ITEM_UNIT_PRICE);

            // Tomamos el ID del medio de pago elegido por el usuario.
            String paymentMethodId = paymentMethod.getId();

            // Esto no se usa hoy en dia.
            Long campaignId = (discount != null) ? discount.getId() : null;

            // Armamos el objeto que va a estar llegando a los servidores de Clinc
            MerchantPayment payment = new MerchantPayment(item, installments, cardIssuerId,
                    token, paymentMethodId, campaignId, DUMMY_MERCHANT_ACCESS_TOKEN);

            // Creamos el pago yendo a los servidores de Clinc
            ErrorHandlingCallAdapter.MyCall<Payment> call = MerchantServer.createPayment(activity, DUMMY_MERCHANT_BASE_URL, DUMMY_MERCHANT_CREATE_PAYMENT_URI, payment);
            call.enqueue(new ErrorHandlingCallAdapter.MyCallback<Payment>() {



                /*
                * Qué tienen que hacer en su servidor?
                * https://www.mercadopago.com.ar/developers/en/solutions/payments/custom-checkout/charge-with-creditcard/android#charge
                * */



                @Override
                public void success(Response<Payment> response) {


                    // Tomamos la respuesta del pago para poder mostrar una pantalla de información al user.
                    new MercadoPago.StartActivityBuilder()
                            .setActivity(activity)
                            .setPayment(response.body())
                            .setPaymentMethod(paymentMethod)
                            .startCongratsActivity();
                }

                @Override
                public void failure(ApiException apiException) {

                    LayoutUtil.showRegularLayout(activity);
                    Toast.makeText(activity, apiException.getMessage(), Toast.LENGTH_LONG).show();
                }
            });
        } else {

            Toast.makeText(activity, "Invalid payment method", Toast.LENGTH_LONG).show();
        }
    }
}
