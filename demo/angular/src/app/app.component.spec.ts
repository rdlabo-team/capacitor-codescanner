import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { provideIonicAngular } from '@ionic/angular/standalone';

import { AppComponent } from './app.component';

describe('AppComponent', () => {
  beforeEach(async () => {
    TestBed.configureTestingModule({
      imports: [AppComponent],
      providers: [provideIonicAngular(), provideRouter([])],
    });
  });

  it('should create the app', () => {
    const fixture = TestBed.createComponent(AppComponent);
    const app = fixture.debugElement.componentInstance;
    expect(app).toBeTruthy();
  });

  // TODO: add more tests!
});
