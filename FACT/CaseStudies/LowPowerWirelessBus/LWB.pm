dtmc

//PARAMETER: Probability of receiving a message
const double pm;

//Script variable parameters
const int k_max = 6; // from 1 up to 6
const int period=60000; //T from 1000 to 60000

const double ps = pm;
const int max_m = 3;
const int states = 7;
const int round = 1000; // T_l
const int remaining = period - round; //T- T_l
const int schedule_length = 15; //T_s
const int base_guard = 4;
const double data_time = 100*k_max+10;//T_c radio on-time for data and contention slots


module LWB_node

q : [0..states] init 0; //device state
m : [0..max_m] init 0;  //missed schedules

//B_end
[end] q = 0 -> ps:(q'=2) + (1-ps):(q'= 1);

//B_begin
[begin] q = 1 -> 1:(q'=0);

//R_begin
[begin] q = 2 -> ps:(q'= 3) + (1-ps):(q'= 0);

//R_end
[end] q = 3 -> ps:(q'= 4) + (1-ps):(q'= 1);

//S_begin
[begin] q = 4 -> ps:(q'= 5) + (1-ps):(q'= 6) & (m'= 1);

//S_e
[end] q = 5 -> ps:(q'= 4) + (1-ps):(q'= 7) & (m'= 1);
	
//M_end
[end] (q = 6) & (m<max_m)  -> ps:(q'= 4) & (m'= 0) + (1-ps):(q'= 7) & (m'= m+1);

//M_end_max
[end] (q = 6) & (m>=max_m)  -> ps:(q'= 4) & (m'= 0) + (1-ps):(q'= 1) & (m'= 0);

//M_begin
[begin] (q = 7) & (m<max_m)  -> ps:(q'= 5) & (m' =0) + (1-ps):(q'= 6) & (m'= m+1);

//M_begin_max
[begin] (q = 7) & (m>=max_m)  -> ps:(q'= 5) & (m'=0) + (1-ps):(q'= 0) & (m'= 0);


endmodule


module LWB_host

b :  bool init false; // LWB host alternatively sends begin and end schedules.

[begin] b -> 1: (b'=!b);
[end] !b -> 1: (b'=!b);

endmodule


rewards "power"
	q=0 : remaining/period; //% per periodo nel tempo di accensione radio
	q=1 : round/period;
	q=2 : (schedule_length + data_time + base_guard + (max_m+1)*(max_m+1))/period;
	q=3 : (schedule_length + base_guard +(max_m+1)*(max_m+1))/period;
	q=4 : (schedule_length + data_time + base_guard)/period;
	q=5 : (schedule_length + base_guard)/period;
	q=6 : (schedule_length + base_guard+(m+1)*(m+1))/period;
	q=7 & m>1 :  (schedule_length + base_guard+(m+1)*(m+1))/period;
	q=7 & m<=1 :  (schedule_length + data_time + base_guard+(m+1)*(m+1))/period; 
endrewards

rewards "energy" //accensione radio ms
	q=0 : remaining;
	q=1 : round;
	q=2 : (schedule_length + data_time + base_guard + (max_m+1)*(max_m+1));
	q=3 : (schedule_length + base_guard +(max_m+1)*(max_m+1));
	q=4 : (schedule_length + data_time + base_guard);
	q=5 : (schedule_length + base_guard);
	q=6 : (schedule_length + base_guard+(m+1)*(m+1));
	q=7 & m>1 :  (schedule_length + base_guard+(m+1)*(m+1));
	q=7 & m<=1 :  (schedule_length + data_time + base_guard+(m+1)*(m+1)); 
endrewards
