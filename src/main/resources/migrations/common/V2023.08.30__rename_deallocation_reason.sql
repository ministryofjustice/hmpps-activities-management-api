UPDATE allocation
   SET deallocated_reason = 'TEMPORARILY_RELEASED'
 WHERE deallocated_reason = 'TEMPORARY_ABSENCE';
