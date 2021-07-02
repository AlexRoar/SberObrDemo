require: utils/utils.js

# https://developer.sberdevices.ru/docs/ru/developer_tools/ide/common/bot_structure/yaml
require: dicts/cartItems.yaml
    var = $CartItems

#patterns:
#    $hello = (салют|привет|здравствуй*|здарова|добрый (день|вечер) | *start)
    
theme: /

    state: Fallback
        event!: noMatch
        script:
            $response.replies = $response.replies || [];
            $response.replies.push({
                type: 'raw',
                body: {
                    pronounceText: "Я не понимаю: {{$parseTree.text}}",
                    items: [
                        {
                            command: {
                                type: "smart_app_data",
                                smart_app_data: {
                                    type: "app_action",
                                    message: "Я не понимаю: {{$parseTree.text}}"
                                },
                            },
                            auto_listening: true
                        },
                    ],
                },
            });
        
    # этот стейт нужен для запуска нейтив аппа на устройстве
    # эвент старта приходит сюда, а отсюда мы отсылаем сообщение (контент не важен)
    # которое приводит к запуску нейтив аппа на устройстве
    state: runApp
        event!: runApp
        q!: * *start
        script:
            $response.replies = $response.replies || [];
            $response.replies.push({
                type: 'raw',
                body: {
                    items: [
                        {
                            command: {
                                type: "smart_app_data",
                                smart_app_data: {
                                    type: "app_action",
                                    message: "запустиприложение"
                                },
                            },
                            auto_listening: true
                        },
                    ],
                },
            });
        
    state: Какиевещи
        q!: Что надето*
        q!: Какие вещи*
        script:
            $jsapi.log("LOG of state")
            $jsapi.log(JSON.stringify($request.rawRequest))
            var myState = $request.rawRequest.payload.meta.current_app.state.myState
            if (myState) {
                $reactions.answer("" + myState);
            } else {
                $reactions.answer("Никаких");
            }
            
    state: Естьлишапка
        q!: Есть ли шапка*
        script:
            $jsapi.log("Надета ли шапка " + myState.indexOf("шапка") !== -1)

    state: ЧтоНадеть
        intent!: /dressUp
        script: 
            var whatToWear = $parseTree["одежда"][0]["value"]
            reply( {
                        "pronounceText": whatToWear + " надели на андроида",
                        items: [ 
                            formWearCommand(whatToWear) 
                        ]
                    },
                    $response);
                    
    state: СнятьВещи
        intent!: /removeClothes
        script:
            reply( {
                        "pronounceText": "Все вещи сняты",
                        items: [ 
                            formClearClothesCommand() 
                        ]
                    },
                    $response);

    # секция демо покупки предметов
    state: КупиСлона
        intent!: /buySomething
        script:
            // очищаем корзину
            $payment.clearItems()
            
            // добавляем продукт в корзину
             $payment.addItem($CartItems.Elephant.card_info)
            
            # создание счёта, получаем invoice_id
            var response = $payment.createPayment(
                    createOrder(
                        create_UUID(), 
                        $injector.service_id
                    )
                );
            $jsapi.log("Response: " + JSON.stringify(response))
            $session.invoice_id = response.invoice_id; 
            
            $reactions.pay($session.invoice_id);
        
    # получаем ответ от ассистента о результате оплаты, этот стейт вызывает paylib.js
    state: PayDialogFinished
        event!: PAY_DIALOG_FINISHED
        q!: PAY_DIALOG_FINISHED
        script:
            try {
                $temp.code = $request.data.eventData.payment_response.response_code;
                $jsapi.log("check invoice: response_code = "+$temp.code + "; device = "+$session.surface);
                
                $reactions.transition("/ShowPaymentStatus");
            } catch(e) {
                $jsapi.log("catch(e)" + e.message);
                $reactions.transition("/PaymentStatusError");
            }
            
    # проверяем статус платежа
    state: ShowPaymentStatus
        intent!: /checkStatus
        script:
            var response = $payment.checkPayment($session.invoice_id);
            $temp.code = response.invoice_status;
            var user_message = response.error.user_message
            
            if ($temp.code == "confirmed") {
                $session.isElephantBought = true
                
                reply( {
                    "pronounceText": "Поздравляю вас! Слоняра успешно приобретен",
                    items: [ 
                            formSuccessBuyCommand()
                        ]
                    },
                    $response);
            } else {
                $session.isElephantBought = false;
                
                reply( {
                    "pronounceText": "К сожалению слон не куплен. Статус инвойса: " + user_message,
                    items: [ 
                            formFailBuyCommand()
                        ]
                    },
                    $response);
            }

    # пример взаимодействия с пользователем, когда в пределах сессии была покупка
    # нужно заметить, что информация об этой покупке хранится именно в рамках сессии
    state: IsElephantHere
        intent!: /hasElephant
        script:
            if ($session.isElephantBought == true) {
                $session.elephantName = "Да, есть слон"
            } else {
                $session.elephantName = "Вы знаете, слона нету к сожалению"
            }
        a: Привет, {{ $session.elephantName }}!
            
        
    state: PaymentStatusError
        a: Ошибка платежа
        
    state: ActionNativeApp
        event!: ACTION_FROM_NATIVE_APP
        script:
            var request = $jsapi.context().request;
            var eventData = request.data.eventData;
            var eventDataParameters = eventData.myParameter;
            $reactions.answer("Нажат обьект: " + eventDataParameters);
            
            
            
# тест из смарт апп кода