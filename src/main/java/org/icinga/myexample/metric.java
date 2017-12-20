
package org.icinga.myexample;

public enum metric {
  ping_wpl,
  ping_timeout,
  dns_timeout,

  tcp_address,
  tcp_port,
  tcp_timeout,
  udp_address,
  udp_port, 

  ssh_ address,
  ssh_timeout,

  procs_warning,
  procs_critical,
  procs_timeout,

  load_wload15,
  load_cload15,

  mem_used,
  mem_free,
  mem_critical,

  disk_wfree,
  disk_cfree,
  disk_errors_only,
  disk_timeout
}
