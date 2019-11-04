# Coupon Rewarding CorDaap

* Persistent emee Admin Pays certain amount, say 10000 rupees to AmazonPay to buy 10000 amazon pay coupon rewards.
* AmazonPay immediately Credits 10000 amazon pay to Persistent emee with unique Transaction Id.
* The coupon amount is updated in the state where the participants are Persistent emee and AmazonPay.
* Any Team Leads in persistent can now generate coupon for particular employee using their email id and emp id.
* Both Persistent system and amazonPay records immediately that coupon worth 1000 Rs has been allocated to employee with certain email     id   and emp Id. Recording here mean State is updated.
* Coupon is valid for 1year upon generation, Coupon is not transferable, Coupon can only be redeemed fully, coupon can only be redeemed   by particular employee, this thing is taken care by Smart contract.
* Once the coupon is redeemed both amazon and persistent record that certain amount has been redeemed by employee and both update the     commonly shared ledger State with the remaining amount. 
 

## Minimum System Requirements
* 16 GB RAM preferably
* Latest version of JAVA 8 java 8u181 (Preferably, Corda and kotlin support latest version of java 8)
* http://docs.corda.r3.com/sizing-and-performance.html 

## Instructions for setting up
1. clone the repository https://github.com/shivanzo/Finance-Assignment-Final-Phase
2. To build on unix : ./gradlew deployNodes
3. To build on windows : gradlew.bat deployNodes
4. For running corDapp on unix ./runnodes --log-to-console --logging-level=DEBUG
5. For running corDapp on windows runnodes.bat --log-to-console --logging-level=DEBUG
6. Good to run in intellij

## Accessing Using UI
                                                                                                                                  
* http://localhost:10009/web/example/recharge-amount.html  :   Persistent Emee/Zaggle Makes One time recharge to partner vendor 
* http://localhost:10009/web/example/                      :   Persistent Dashboard for Team Lead to Assign coupon for Employee 
* http://localhost:10012/web/example/amazon-pay.html       :   AmazonPay dashboard for employee to redeem the coupon assigned    
* http://localhost:10015/web/example/phone-pay.html        :   PhonePay dashboard for employee to redeem phonepe coupon assigned 
* http://localhost:10009/web/example/view-coupons.html     :   Admin dashboard to view coupon Available                         
* http://localhost:10009/web/example/view-recharges.html   :   Admin panel to view recharge money available with vendor            

## Accessing over API endpoints 

| Node                      |    Port         |
| ------------------------- | --------------- | 
| Persistent emee/ Zaggle   | localhost:10009 |
| AmazonPay wallet          | localhost:10012 |      
| PhonePe wallet            | localhost:10015 |   





