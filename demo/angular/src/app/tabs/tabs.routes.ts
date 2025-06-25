import { NgModule } from '@angular/core';
import type { Routes } from '@angular/router';
import { RouterModule  } from '@angular/router';

import { TabsPage } from './tabs.page';
import {Tab1Page} from '../tab1/tab1.page';
import {Tab2Page} from '../tab2/tab2.page';
import {Tab3Page} from '../tab3/tab3.page';

export const routes: Routes = [
  {
    path: 'tabs',
    component: TabsPage,
    children: [
      {
        path: 'tab1',
        children: [
          {
            path: '',
            component: Tab1Page,
          },
        ],
      },
      {
        path: 'tab2',
        children: [
          {
            path: '',
            component: Tab2Page,
          },
        ],
      },
      {
        path: 'tab3',
        children: [
          {
            path: '',
            component: Tab3Page,
          },
        ],
      },
      {
        path: '',
        redirectTo: '/tabs/tab1',
        pathMatch: 'full',
      },
    ],
  },
  {
    path: '',
    redirectTo: '/tabs/tab1',
    pathMatch: 'full',
  },
];
