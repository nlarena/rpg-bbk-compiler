**FREE
dcl-s counter int(10) inz(0);
dcl-s name char(50);
dcl-ds customer qualified;
  id int(10);
  active ind;
end-ds;
counter = counter + 1;
if counter > 0 and customer.active = *on;
  name = 'active';
  for i = 1 to counter;
    counter = counter - 1;
  endfor;
endif;
